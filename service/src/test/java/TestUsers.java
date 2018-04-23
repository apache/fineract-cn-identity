/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.identity.api.v1.domain.*;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
import org.apache.fineract.cn.test.env.TestEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.fineract.cn.identity.internal.util.IdentityConstants.SU_NAME;
import static org.apache.fineract.cn.identity.internal.util.IdentityConstants.SU_ROLE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Myrle Krantz
 */
public class TestUsers extends AbstractComponentTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("src/doc/generated-snippets/test-users");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  final String path = "/identity/v1";

  @Before
  public void setUp(){

    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
            .build();
  }

  @Test
  public void testAddLogin() throws InterruptedException {

    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    try (final AutoUserContext ignore = loginAdmin()) {
      final User user = getTestSubject().getUser(username);
      Assert.assertNotNull(user);
      Assert.assertEquals("Correct user identifier?", username, user.getIdentifier());
      Assert.assertEquals("Correct role?", ADMIN_ROLE, user.getRole());
    }

    final Authentication userAuthentication =
            getTestSubject().login(username, TestEnvironment.encodePassword(AHMES_PASSWORD));

    Assert.assertNotNull(userAuthentication);

    try (final AutoUserContext ignored = new AutoUserContext(username, userAuthentication.getAccessToken())) {
      getTestSubject().createUser(new UserWithPassword("Ahmes_friend", "scribe",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD)));

      final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_USER, "Ahmes_friend");
      Assert.assertTrue(found);
    }

    try (final AutoUserContext ignore = loginAdmin()) {
      final List<User> users = getTestSubject().getUsers();
      Assert.assertTrue(Helpers.instancePresent(users, User::getIdentifier, username));
      Assert.assertTrue(Helpers.instancePresent(users, User::getIdentifier, "Ahmes_friend"));
    }

    try {
      this.mockMvc.perform(post(path + "/users/")
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE))
              .andExpect(status().is4xxClientError());
    } catch (Exception e) {e.printStackTrace();}
  }

  @Test
  public void testChangeUserRole() throws InterruptedException {
    final String userIdentifier = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication ahmesAuthentication =
            getTestSubject().login(userIdentifier, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userIdentifier, ahmesAuthentication.getAccessToken())) {
      List<User> users = getTestSubject().getUsers();
      Assert.assertEquals(2, users.size());

      getTestSubject().changeUserRole(userIdentifier, new RoleIdentifier("scribe"));

      final boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_USER_ROLEIDENTIFIER, userIdentifier);
      Assert.assertTrue(found);

      final User ahmes = getTestSubject().getUser(userIdentifier);
      Assert.assertEquals("scribe", ahmes.getRole());

      final Set<Permission> userPermittableGroups = getTestSubject().getUserPermissions(userIdentifier);
      Assert.assertTrue(userPermittableGroups.contains(new Permission(PermittableGroupIds.SELF_MANAGEMENT, AllowedOperation.ALL)));

      users = getTestSubject().getUsers();
      Assert.assertEquals(2, users.size());
    }

    try
    {
      this.mockMvc.perform(put(path + "/users/" + userIdentifier )
              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().is4xxClientError());
    } catch (Exception E){ E.printStackTrace();}
  }

  @Test
  public void testChangeAntonyRoleFails() throws InterruptedException {
    final String userIdentifier = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication ahmesAuthentication =
            getTestSubject().login(userIdentifier, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userIdentifier, ahmesAuthentication.getAccessToken())) {
      try {
        getTestSubject().changeUserRole(SU_NAME, new RoleIdentifier("scribe"));
        Assert.fail("Should not be able to change the role set for antony.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final User antony = getTestSubject().getUser(SU_NAME);
      Assert.assertEquals(SU_ROLE, antony.getRole());
    }
  }

  @Test
  public void testAdminProvisioning() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final List<Role> roleIdentifiers = getTestSubject().getRoles();
      Assert.assertTrue(Helpers.instancePresent(roleIdentifiers, Role::getIdentifier, ADMIN_ROLE));

      final Role role = getTestSubject().getRole(ADMIN_ROLE);
      Assert.assertNotNull(role);
      Assert.assertTrue(role.getPermissions().contains(constructFullAccessPermission(PermittableGroupIds.IDENTITY_MANAGEMENT)));
      Assert.assertTrue(role.getPermissions().contains(constructFullAccessPermission(PermittableGroupIds.ROLE_MANAGEMENT)));

      final List<User> userIdentifiers = getTestSubject().getUsers();
      Assert.assertTrue(Helpers.instancePresent(userIdentifiers, User::getIdentifier, ADMIN_IDENTIFIER));

      final User user = getTestSubject().getUser(ADMIN_IDENTIFIER);
      Assert.assertNotNull(user);
      Assert.assertEquals(ADMIN_IDENTIFIER, user.getIdentifier());
      Assert.assertEquals(ADMIN_ROLE, user.getRole());

      final Set<Permission> adminPermittableGroups = getTestSubject().getUserPermissions(ADMIN_IDENTIFIER);
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.SELF_MANAGEMENT, AllowedOperation.ALL)));
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, AllowedOperation.ALL)));
      Assert.assertTrue(adminPermittableGroups.contains(new Permission(PermittableGroupIds.ROLE_MANAGEMENT, AllowedOperation.ALL)));
    }
  }

  private Permission constructFullAccessPermission(final String permittableGroupId) {
    final HashSet<AllowedOperation> allowedOperations = new HashSet<>();
    allowedOperations.add(AllowedOperation.CHANGE);
    allowedOperations.add(AllowedOperation.DELETE);
    allowedOperations.add(AllowedOperation.READ);
    return new Permission(permittableGroupId, allowedOperations);
  }
}
