package edu.itc.cloud.service;

import edu.itc.cloud.model.FileEntity;
import edu.itc.cloud.model.Folder;
import edu.itc.cloud.model.User;
import edu.itc.cloud.repository.FileRepository;
import edu.itc.cloud.repository.FolderRepository;
import edu.itc.cloud.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Files &amp; folders within a user's own storage. Every method takes the acting
 * {@link User} and verifies ownership, so a user can never touch another user's
 * data (per-user isolation).
 */
@Service
public class StorageService {

    private final FileRepository files;
    private final FolderRepository folders;
    private final UserRepository users;

    public StorageService(FileRepository files, FolderRepository folders, UserRepository users) {
        this.files = files;
        this.folders = folders;
        this.users = users;
    }

    // ----- Folders --------------------------------------------------------

    @Transactional
    public Folder createFolder(User owner, String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Folder name must not be blank");
        }
        if (parentId != null) {
            requireFolder(owner, parentId); // parent must belong to the same user
        }
        return folders.save(new Folder(owner.getId(), name, parentId));
    }

    @Transactional(readOnly = true)
    public List<Folder> listFolders(User owner, Long parentId) {
        return folders.findByOwnerIdAndParentIdOrderByNameAsc(owner.getId(), parentId);
    }

    @Transactional
    public Folder renameFolder(User owner, Long folderId, String newName) {
        Folder folder = requireFolder(owner, folderId);
        folder.setName(newName);
        return folders.save(folder);
    }

    @Transactional
    public void deleteFolder(User owner, Long folderId) {
        Folder folder = requireFolder(owner, folderId);
        // Remove files inside this folder (and reclaim their space).
        for (FileEntity f : files.findByOwnerIdAndFolderIdOrderByNameAsc(owner.getId(), folderId)) {
            deleteFile(owner, f.getId());
        }
        folders.delete(folder);
    }

    // ----- Files ----------------------------------------------------------

    /** Upload a file, enforcing the quota. Throws {@link QuotaExceededException} if it would not fit. */
    @Transactional
    public FileEntity upload(User owner, Long folderId, String name, byte[] content) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name must not be blank");
        }
        if (folderId != null) {
            requireFolder(owner, folderId);
        }
        long size = content == null ? 0 : content.length;
        long free = freeBytes(owner);
        if (size > free) {
            throw new QuotaExceededException(size, free);
        }
        FileEntity saved = files.save(
                new FileEntity(owner.getId(), folderId, name, content, Instant.now()));
        adjustUsage(owner, size);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FileEntity> listFiles(User owner, Long folderId) {
        return files.findByOwnerIdAndFolderIdOrderByNameAsc(owner.getId(), folderId);
    }

    /** Convenience: just the file names in a folder (root when folderId is null). */
    @Transactional(readOnly = true)
    public List<String> list(User owner, Long folderId) {
        return listFiles(owner, folderId).stream().map(FileEntity::getName).toList();
    }

    @Transactional(readOnly = true)
    public byte[] download(User owner, Long fileId) {
        return requireFile(owner, fileId).getContent();
    }

    @Transactional
    public FileEntity rename(User owner, Long fileId, String newName) {
        FileEntity file = requireFile(owner, fileId);
        file.setName(newName);
        return files.save(file);
    }

    @Transactional
    public void deleteFile(User owner, Long fileId) {
        FileEntity file = requireFile(owner, fileId);
        long size = file.getSizeBytes();
        files.delete(file);
        adjustUsage(owner, -size);
    }

    /** Wipe everything a user owns (used when deleting an account). */
    @Transactional
    public void deleteAllFor(User owner) {
        files.deleteByOwnerId(owner.getId());
        folders.deleteByOwnerId(owner.getId());
    }

    // ----- Quota & links --------------------------------------------------

    @Transactional(readOnly = true)
    public long usedBytes(User owner) {
        return users.findById(owner.getId())
                .orElseThrow(() -> new NotFoundException("No such user"))
                .getUsedBytes();
    }

    @Transactional(readOnly = true)
    public long freeBytes(User owner) {
        User fresh = users.findById(owner.getId())
                .orElseThrow(() -> new NotFoundException("No such user"));
        return fresh.getQuotaBytes() - fresh.getUsedBytes();
    }

    /** Build a deterministic share link of the form {@code /s/{8 hex chars}}. */
    public String createShareLink(Long fileId) {
        String token = String.format("%08x", (fileId * 2654435761L) & 0xffffffffL);
        return "/s/" + token;
    }

    // ----- helpers --------------------------------------------------------

    private void adjustUsage(User owner, long delta) {
        User fresh = users.findById(owner.getId())
                .orElseThrow(() -> new NotFoundException("No such user"));
        fresh.setUsedBytes(Math.max(0, fresh.getUsedBytes() + delta));
        users.save(fresh);
        owner.setUsedBytes(fresh.getUsedBytes()); // keep the caller's copy in sync
    }

    private Folder requireFolder(User owner, Long folderId) {
        Folder folder = folders.findById(folderId)
                .orElseThrow(() -> new NotFoundException("No such folder: " + folderId));
        if (!folder.getOwnerId().equals(owner.getId())) {
            throw new AccessDeniedException("Folder " + folderId + " does not belong to you");
        }
        return folder;
    }

    private FileEntity requireFile(User owner, Long fileId) {
        FileEntity file = files.findById(fileId)
                .orElseThrow(() -> new NotFoundException("No such file: " + fileId));
        if (!file.getOwnerId().equals(owner.getId())) {
            throw new AccessDeniedException("File " + fileId + " does not belong to you");
        }
        return file;
    }
}
