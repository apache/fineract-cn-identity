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

import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.api.util.NotFoundException;
import org.apache.fineract.cn.identity.api.v1.domain.Permission;
import org.apache.fineract.cn.identity.api.v1.domain.Role;
import org.apache.fineract.cn.identity.api.v1.events.EventConstants;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.fineract.cn.identity.internal.util.IdentityConstants.SU_ROLE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author Myrle Krantz
 */
public class TestRoles extends AbstractComponentTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("src/doc/generated-snippets/test-roles");

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
  public void testRolesSortedAlphabetically() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Permission rolePermission = buildRolePermission();

      final Role role1 = buildRole(testEnvironment.generateUniqueIdentifer("abba"), rolePermission);
      final Role role2 = buildRole(testEnvironment.generateUniqueIdentifer("bubba"), rolePermission);
      final Role role3 = buildRole(testEnvironment.generateUniqueIdentifer("c1"), rolePermission);
      final Role role4 = buildRole(testEnvironment.generateUniqueIdentifer("calla"), rolePermission);
      final Role role5 = buildRole(testEnvironment.generateUniqueIdentifer("uelf"), rolePermission);
      final Role role6 = buildRole(testEnvironment.generateUniqueIdentifer("ulf"), rolePermission);

      getTestSubject().createRole(role2);
      getTestSubject().createRole(role1);
      getTestSubject().createRole(role6);
      getTestSubject().createRole(role4);
      getTestSubject().createRole(role3);
      getTestSubject().createRole(role5);

      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role1.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role2.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role3.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role4.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role5.getIdentifier()));
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, role6.getIdentifier()));

      final List<Role> roles = getTestSubject().getRoles();
      final List<String> idList = roles.stream().map(Role::getIdentifier).collect(Collectors.toList());
      final List<String> sortedList = Arrays.asList(
              role1.getIdentifier(),
              role2.getIdentifier(),
              role3.getIdentifier(),
              role4.getIdentifier(),
              role5.getIdentifier(),
              role6.getIdentifier());
      final List<String> filterOutIdsFromOtherTests = idList.stream().filter(sortedList::contains).collect(Collectors.toList());
      Assert.assertEquals(sortedList, filterOutIdsFromOtherTests);
    }
  }

  @Test
  public void testCreateRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = generateRoleIdentifier();

      final Permission rolePermission = buildRolePermission();
      final Role scribe = buildRole(roleIdentifier, rolePermission);

      getTestSubject().createRole(scribe);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribe.getIdentifier());
        Assert.assertTrue(found);
      }

      final List<Role> roles = getTestSubject().getRoles();
      Assert.assertTrue(Helpers.instancePresent(roles, Role::getIdentifier, roleIdentifier));

      final Role role = getTestSubject().getRole(roleIdentifier);
      Assert.assertNotNull(role);
      Assert.assertEquals(roleIdentifier, role.getIdentifier());
      Assert.assertEquals(Collections.singletonList(rolePermission), role.getPermissions());
    }

    try {
      this.mockMvc.perform(post(path + "/roles")
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE))
              .andExpect(status().is4xxClientError());
    } catch (Exception e) {e.printStackTrace();}

  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotBeAbleToCreateRoleNamedDeactivated() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Permission rolePermission = buildRolePermission();
      final Role deactivated = buildRole("deactivated", rolePermission);

      getTestSubject().createRole(deactivated);
    }
  }

  @Test
  public void deleteRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = createRoleManagementRole();

      final Role role = getTestSubject().getRole(roleIdentifier);
      Assert.assertNotNull(role);

      getTestSubject().deleteRole(role.getIdentifier());

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_DELETE_ROLE, roleIdentifier);
        Assert.assertTrue(found);
      }

      final List<Role> roles = getTestSubject().getRoles();
      Assert.assertFalse(Helpers.instancePresent(roles, Role::getIdentifier, roleIdentifier));

      try
      {
        this.mockMvc.perform(delete(path + "/roles/" + roleIdentifier)
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
      } catch (Exception exception){ exception.printStackTrace(); }
    }
  }

  @Test(expected= NotFoundException.class)
  public void deleteRoleThatDoesntExist() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String randomIdentifier = generateRoleIdentifier();

      getTestSubject().deleteRole(randomIdentifier);
    }
  }

  @Test()
  public void changeRole() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = createRoleManagementRole();

      final Role role = getTestSubject().getRole(roleIdentifier);
      role.getPermissions().add(buildUserPermission());

      getTestSubject().changeRole(roleIdentifier, role);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_PUT_ROLE, role.getIdentifier());
        Assert.assertTrue(found);
      }

      final Role changedRole = getTestSubject().getRole(roleIdentifier);
      Assert.assertEquals(role, changedRole);

      try
      {
        this.mockMvc.perform(put(path + "/roles/" + roleIdentifier )
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
      } catch (Exception E){ E.printStackTrace();}

    }
  }

  @Test
  public void testChangePharaohRoleFails() throws InterruptedException {
    try (final AutoUserContext ignore = loginAdmin()) {
      final Role referenceRole = getTestSubject().getRole(SU_ROLE);
      final Role roleChangeRequest = buildRole(SU_ROLE, buildSelfPermission());

      try {
        getTestSubject().changeRole(SU_ROLE, roleChangeRequest);
        Assert.fail("Should not be able to change the pharaoh role.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final Role unChangedRole = getTestSubject().getRole(SU_ROLE);
      Assert.assertEquals(referenceRole, unChangedRole);
    }
  }

  @Test
  public void testDeletePharaohRoleFails() throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {
      final Role adminRole = getTestSubject().getRole(ADMIN_ROLE);
      try {
        getTestSubject().deleteRole(ADMIN_ROLE);
        Assert.fail("It should not be possible to delete the admin role.");
      }
      catch (final IllegalArgumentException expected) {
        //noinspection EmptyCatchBlock
      }

      final Role adminRoleStillThere = getTestSubject().getRole(ADMIN_ROLE);
      Assert.assertEquals(adminRole, adminRoleStillThere);
    }
  }
}