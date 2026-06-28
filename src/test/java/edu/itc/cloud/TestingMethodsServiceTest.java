package edu.itc.cloud;

import static edu.itc.cloud.support.TestFiles.MB;
import static edu.itc.cloud.support.TestFiles.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.itc.cloud.model.Folder;
import edu.itc.cloud.model.User;
import edu.itc.cloud.service.AccessDeniedException;
import edu.itc.cloud.service.NotFoundException;
import edu.itc.cloud.service.QuotaExceededException;
import edu.itc.cloud.service.StorageService;
import edu.itc.cloud.service.UserService;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Demonstrates the Lesson 08 testing methods against the service layer.
 *
 * <p>Each {@code @Feature} corresponds to one testing method so the Allure
 * report groups them clearly. See the README for the full mapping table.</p>
 */
@SpringBootTest
@Epic("Private Cloud Storage")
class TestingMethodsServiceTest {

    @Autowired
    private UserService users;

    @Autowired
    private StorageService storage;

    private static final AtomicInteger SEQ = new AtomicInteger();

    /** A fresh, unique user per test keeps cases isolated and repeatable. */
    private User freshUser() {
        String email = "user" + SEQ.incrementAndGet() + "@itc.edu";
        return users.register(email, "Secret123!");
    }

    // 1) CONTENT EQUALS ----------------------------------------------------
    @Test
    @Feature("Content equals")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("New user is granted exactly 50 MB")
    void newUserQuotaEqualsFiftyMb() {
        User u = freshUser();
        assertThat(u.getQuotaBytes()).isEqualTo(50 * MB);
        assertThat(u.getQuotaBytes()).isEqualTo(52_428_800L);
        assertThat(u.getUsedBytes()).isEqualTo(0L);
    }

    // 2) CONTAINS ----------------------------------------------------------
    @Test
    @Feature("Contains")
    @DisplayName("Folder listing contains an uploaded file name")
    void listingContainsUploadedName() {
        User u = freshUser();
        storage.upload(u, null, "report.pdf", bytesOf(MB));
        storage.upload(u, null, "notes.txt", bytesOf(MB));

        List<String> names = storage.list(u, null);
        assertThat(names).contains("report.pdf")
                .doesNotContain("secret.txt");
    }

    // 3) REGEX MATCHED -----------------------------------------------------
    @Test
    @Feature("Regex matched")
    @DisplayName("Share link matches /s/{8 hex} and email is well-formed")
    void shareLinkAndEmailMatchPatterns() {
        User u = freshUser();
        var file = storage.upload(u, null, "photo.png", bytesOf(MB));

        String link = storage.createShareLink(file.getId());
        assertThat(link).matches("/s/[0-9a-f]{8}");
        assertThat(u.getEmail()).matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
    }

    // 4) FORMULA MATCHED ---------------------------------------------------
    @Test
    @Feature("Formula matched")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("free = quota - sum(file sizes)")
    void freeSpaceFollowsFormula() {
        User u = freshUser();
        Allure.step("Upload 10 MB and 15 MB", () -> {
            storage.upload(u, null, "a.bin", bytesOf(10 * MB));
            storage.upload(u, null, "b.bin", bytesOf(15 * MB));
        });

        long usedExpected = (10 + 15) * MB;
        long freeExpected = u.getQuotaBytes() - usedExpected;

        assertThat(storage.usedBytes(u)).isEqualTo(usedExpected);
        assertThat(storage.freeBytes(u)).isEqualTo(freeExpected);
    }

    // 5) PREDICATE ---------------------------------------------------------
    @Test
    @Feature("Predicate")
    @DisplayName("Usage never exceeds quota (custom condition)")
    void usageNeverExceedsQuota() {
        User u = freshUser();
        storage.upload(u, null, "x.bin", bytesOf(20 * MB));

        assertThat(u).matches(
                user -> storage.usedBytes(user) <= user.getQuotaBytes(),
                "never over quota");
    }

    // 6) COLLECTION --------------------------------------------------------
    @Test
    @Feature("Collection")
    @DisplayName("Listing has the right size, order and no duplicates")
    void collectionShapeIsExact() {
        User u = freshUser();
        storage.upload(u, null, "c.txt", bytesOf(MB));
        storage.upload(u, null, "a.txt", bytesOf(MB));
        storage.upload(u, null, "b.txt", bytesOf(MB));

        List<String> names = storage.list(u, null); // repository sorts by name asc
        assertThat(names)
                .hasSize(3)
                .containsExactly("a.txt", "b.txt", "c.txt")
                .doesNotHaveDuplicates();
    }

    // 7) EXCEPTION ---------------------------------------------------------
    @Test
    @Feature("Exception")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Upload beyond quota is rejected; a fitting upload is not")
    void overQuotaThrows() {
        User u = freshUser();

        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> storage.upload(u, null, "huge.bin", bytesOf(60 * MB)));
        assertThat(ex.getMessage()).contains("quota")
                .matches(".*\\d+ MB.*");

        assertDoesNotThrow(() -> storage.upload(u, null, "ok.bin", bytesOf(5 * MB)));
    }

    // 8) TOLERANCE / RANGE -------------------------------------------------
    @Test
    @Feature("Tolerance and range")
    @DisplayName("Used MB is close to expected; free stays within range")
    void toleranceAndRange() {
        User u = freshUser();
        storage.upload(u, null, "d.bin", bytesOf(25 * MB));

        double usedMb = storage.usedBytes(u) / 1_048_576.0;
        assertThat(usedMb).isCloseTo(25.0, within(0.01));
        assertThat(storage.freeBytes(u)).isBetween(0L, u.getQuotaBytes());
    }

    // 9) ISOLATION (predicate + collection + exception together) -----------
    @Test
    @Feature("User isolation")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A user cannot see or touch another user's files")
    void usersAreIsolated() {
        User a = freshUser();
        User b = freshUser();
        var secret = storage.upload(a, null, "secret.txt", bytesOf(MB));

        assertThat(storage.list(b, null)).isEmpty();
        assertThat(b).matches(x -> storage.usedBytes(x) == 0L, "b uses nothing");
        assertThrows(AccessDeniedException.class,
                () -> storage.download(b, secret.getId()));
    }

    // 10) ACCOUNT DELETION reclaims everything -----------------------------
    @Test
    @Feature("Account deletion")
    @DisplayName("Deleting an account removes the user and their data")
    void deleteAccountWipesData() {
        User u = freshUser();
        Folder docs = storage.createFolder(u, "Documents", null);
        storage.upload(u, docs.getId(), "keep.txt", bytesOf(2 * MB));
        Long userId = u.getId();

        users.deleteAccount(userId);

        // The account no longer exists (exception method) ...
        assertThrows(NotFoundException.class, () -> users.require(userId));
        // ... and its folders are gone too.
        assertThat(storage.listFolders(u, null)).isEmpty();
    }
}
