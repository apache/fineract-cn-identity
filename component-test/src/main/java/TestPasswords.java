/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.core.test.domain.TimeStampChecker;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.identity.api.v1.domain.Authentication;
import io.mifos.identity.api.v1.domain.Password;
import io.mifos.identity.api.v1.domain.UserWithPassword;
import io.mifos.identity.api.v1.events.EventConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * @author Myrle Krantz
 */
public class TestPasswords extends AbstractComponentTest {

  @Test
  public void testAdminChangeUserPassword() throws InterruptedException {
    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    try (final AutoUserContext ignore = loginAdmin()) {
      final String newPassword = TestEnvironment.encodePassword(
              AHMES_PASSWORD + "make_it_a_little_longer");

      {
        //Important here is that we are changing the password *as*the*admin*.
        getTestSubject().changeUserPassword(username, new Password(newPassword));
        boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, username);
        Assert.assertTrue(found);
      }

      final Authentication newPasswordAuthentication = getTestSubject().login(username, newPassword);
      try (final AutoUserContext ignore2 = new AutoUserContext(username, newPasswordAuthentication.getAccessToken()))
      {
        getTestSubject().createUser(new UserWithPassword("Ahmes_friend", "scribe",
                TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD)));
        Assert.fail("createUser should've thrown an exception because the password is admin reset.");
      }
      catch (final NotFoundException ex)
      {
        //Should throw because under the new password, the user has only the right to change the password.
      }

      try (final AutoUserContext ignore3 = new AutoUserContext(username, newPasswordAuthentication.getAccessToken()))
      {
        getTestSubject().changeUserPassword(username, new Password(newPassword));
        boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, username);
        Assert.assertTrue(found);
      }

      final Authentication newPasswordAuthenticationAsFullyPermissionedUser = getTestSubject().login(username, newPassword);
      try (final AutoUserContext ignore4 = new AutoUserContext(username, newPasswordAuthenticationAsFullyPermissionedUser.getAccessToken()))
      {
        //Now it should be possible to create a user since the user changed the password herself.
        getTestSubject().createUser(new UserWithPassword("Ahmes_friend", "scribe",
                TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD)));
      }
    }
  }

  @Test
  public void testAdminChangeAdminPassword() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String newPassword = TestEnvironment.encodePassword(
              ADMIN_PASSWORD + "make_it_a_little_longer");

      {
        getTestSubject().changeUserPassword(ADMIN_IDENTIFIER, new Password(newPassword));

        final boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, ADMIN_IDENTIFIER);
        Assert.assertTrue(found);
      }

      try {
        final String oldPassword = TestEnvironment.encodePassword(ADMIN_PASSWORD);
        getTestSubject().login(ADMIN_IDENTIFIER, oldPassword);
        Assert.fail("Login with the old password should not succeed.");
      } catch (final NotFoundException ignored) {
      }

      getTestSubject().login(ADMIN_IDENTIFIER, newPassword);

      {
        //Change the password back so the tests after this don't fail.
        getTestSubject().changeUserPassword(ADMIN_IDENTIFIER, new Password(TestEnvironment.encodePassword(ADMIN_PASSWORD)));
        boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, ADMIN_IDENTIFIER);
        Assert.assertTrue(found);
      }
    }
  }

  @Test
  public void testUserChangeOwnPasswordButNotAdminPassword() throws InterruptedException {
    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, "scribe");

    try (final AutoUserContext ignored = loginUser(username, AHMES_PASSWORD))
    {
      final String newPassword = "new password";
      {
        getTestSubject().changeUserPassword(username, new Password(TestEnvironment.encodePassword(newPassword)));

        boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, username);
        Assert.assertTrue(found);
      }

      final TimeStampChecker passwordExpirationChecker = TimeStampChecker.inTheFutureWithWiggleRoom(Duration.ofDays(93), Duration.ofHours(24));
      final Authentication userAuthenticationAfterPasswordChange = getTestSubject().login(username, TestEnvironment.encodePassword(newPassword));
      final String passwordExpiration = userAuthenticationAfterPasswordChange.getPasswordExpiration();
      passwordExpirationChecker.assertCorrect(passwordExpiration);

      //noinspection EmptyCatchBlock
      try {
        getTestSubject().changeUserPassword(ADMIN_IDENTIFIER, new Password(TestEnvironment.encodePassword(newPassword)));
        Assert.fail("trying to change the admins password should fail.");
      }
      catch (final NotFoundException ex) {
        boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_PASSWORD, ADMIN_IDENTIFIER);
        Assert.assertFalse(found);
      }


      try {
        getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(newPassword));
        Assert.fail("logging into admin with the new password should likewise fail.");
      }
      catch (final NotFoundException ex) {
        //Not found is expected.
      }

      //noinspection EmptyTryBlock
      try (final AutoUserContext ignored2 = loginAdmin()) { //logging into admin with the old password should *not* fail.
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void loginWithUnencodedPasswordShouldThrowIllegalArgumentException() throws InterruptedException {

    try (final AutoUserContext ignored = loginAdmin()) {
      final String selfManagementRoleId = createSelfManagementRole();

      final String userPassword = RandomStringUtils.randomAlphanumeric(5);
      final String userid = createUserWithNonexpiredPassword(userPassword, selfManagementRoleId);

      getTestSubject().login(userid, userPassword);
    }

  }

  @Test
  public void activatedAntonyPasswordDoesntExpire() throws InterruptedException {

    try (final AutoUserContext ignored = loginAdmin()) {
      final Authentication adminAuthentication =
              getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));
      Assert.assertEquals(null, adminAuthentication.getPasswordExpiration());
    }
  }

  @Test
  public void onlyAntonyCanSetAntonyPassword() throws InterruptedException {
    try (final AutoUserContext ignored = loginAdmin()) {

      final String roleIdentifier = createRole(buildUserPermission(), buildSelfPermission(), buildRolePermission());
      final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, roleIdentifier);

      try (final AutoUserContext ignored2 = loginUser(username, AHMES_PASSWORD)) {
        getTestSubject().changeUserPassword(ADMIN_IDENTIFIER, new Password(TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD)));
        Assert.fail("Should not be able to change antony's password from any account other than antony's.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }
    }

    getTestSubject().login(ADMIN_IDENTIFIER, TestEnvironment.encodePassword(ADMIN_PASSWORD));
  }
}
