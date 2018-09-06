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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.cn.anubis.api.v1.domain.AllowedOperation;
import org.apache.fineract.cn.anubis.api.v1.domain.PermittableEndpoint;
import org.apache.fineract.cn.anubis.api.v1.domain.Signature;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.identity.api.v1.domain.*;
import org.apache.fineract.cn.identity.api.v1.events.*;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
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

import java.util.Collections;
import java.util.List;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;


public class IdentityApiDocumentation extends AbstractIdentityTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-identity");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Before
  public void setUp ( ) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Test
  public void documentCreateRole ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String roleIdentifier = generateRoleIdentifier();
      final Permission rolePermission = buildRolePermission();
      final Role copyist = buildRole(roleIdentifier, rolePermission);

      Gson serializer = new Gson();
      this.mockMvc.perform(post("/roles")
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(serializer.toJson(copyist)))
              .andExpect(status().isAccepted())
              .andDo(document("document-create-role", preprocessRequest(prettyPrint()),
                      requestFields(
                              fieldWithPath("identifier").description("Identifier"),
                              fieldWithPath("permissions[].permittableEndpointGroupIdentifier").description("permittable endpoints"),
                              fieldWithPath("permissions[].allowedOperations").type("Set<AllowedOperation>").description("Set of allowed operations")
                      )
              ));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void documentDeleteRole ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = generateRoleIdentifier();

      final Permission rolePermission = buildRolePermission();
      final Role scribe = buildRole(roleIdentifier, rolePermission);

      getTestSubject().createRole(scribe);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribe.getIdentifier());

      try {
        this.mockMvc.perform(delete("/roles/" + scribe.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-role", preprocessRequest(prettyPrint())));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void documentUpdateRole ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {
      final String roleIdentifier = createRoleManagementRole();

      final Role role = getTestSubject().getRole(roleIdentifier);
      role.getPermissions().add(buildUserPermission());

      Gson gson = new Gson();
      role.getPermissions().remove(0);

      try {
        this.mockMvc.perform(put("/roles/" + role.getIdentifier())
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(gson.toJson(role)))
                .andExpect(status().isAccepted())
                .andDo(document("document-update-role", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Identifier"),
                                fieldWithPath("permissions[].permittableEndpointGroupIdentifier").description("permittable endpoints"),
                                fieldWithPath("permissions[].allowedOperations").type("Set<AllowedOperation>").description("Set of allowed operations")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetRole ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String roleIdentifier = generateRoleIdentifier();

      final Permission rolePermission = buildRolePermission();
      final Role scribe = buildRole(roleIdentifier, rolePermission);

      getTestSubject().createRole(scribe);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribe.getIdentifier());

      try {
        this.mockMvc.perform(get("/roles/" + scribe.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-role", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("Identifier"),
                                fieldWithPath("permissions[].permittableEndpointGroupIdentifier").description("permittable endpoints"),
                                fieldWithPath("permissions[].allowedOperations").type("Set<AllowedOperation>").description("Set of allowed operations")
                        )));
      } catch (Exception e) {
        e.printStackTrace();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void documentFindRoles ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String roleIdentifierOne = generateRoleIdentifier();

      final Permission rolePermission = buildRolePermission();
      final Role scribeOne = buildRole(roleIdentifierOne, rolePermission);

      getTestSubject().createRole(scribeOne);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribeOne.getIdentifier());

      final String roleIdentifierTwo = generateRoleIdentifier();

      final Permission userPermission = buildUserPermission();
      final Role scribeTwo = buildRole(roleIdentifierTwo, userPermission);

      List <Role> roles = Lists.newArrayList(scribeOne, scribeTwo);
      roles.stream()
              .forEach(scribe -> {
                try {
                  super.eventRecorder.wait(EventConstants.OPERATION_POST_ROLE, scribe.getIdentifier());
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              });

      try {
        this.mockMvc.perform(get("/roles")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-find-roles", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].identifier").description("first role's identifier"),
                                fieldWithPath("[].permissions[].permittableEndpointGroupIdentifier").description("first role's roles permittable"),
                                fieldWithPath("[].permissions[].allowedOperations").type("Set<AllowedOperation>").description("Set of first role's allowed operations"),
                                fieldWithPath("[].permissions[1].permittableEndpointGroupIdentifier").description("first role's users permittable"),
                                fieldWithPath("[].permissions[1].allowedOperations").type("Set<AllowedOperation>").description("Set of first role's allowed operations"),
                                fieldWithPath("[].permissions[2].permittableEndpointGroupIdentifier").description("first role's self permittable"),
                                fieldWithPath("[].permissions[2].allowedOperations").type("Set<AllowedOperation>").description("Set of first role's allowed operations"),
                                fieldWithPath("[].permissions[3].permittableEndpointGroupIdentifier").description("first role's app_self permittable"),
                                fieldWithPath("[].permissions[3].allowedOperations").type("Set<AllowedOperation>").description("Set of first role's allowed operations"),
                                fieldWithPath("[1].identifier").description("second role's identifier"),
                                fieldWithPath("[1].permissions[].permittableEndpointGroupIdentifier").description("second role's roles permittable"),
                                fieldWithPath("[1].permissions[].allowedOperations").type("Set<AllowedOperation>").description("Set of second role's allowed operations")
                        )));
      } catch (Exception e) {
        e.printStackTrace();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void documentCreatePGroup ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String identifier = testEnvironment.generateUniqueIdentifer("group");

      final PermittableEndpoint permittableEndpoint = buildPermittableEndpoint();
      final PermittableGroup pgroup = buildPermittableGroup(identifier, permittableEndpoint);

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(post("/permittablegroups")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serializer.toJson(pgroup)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-p-group", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("Permittable group identifier"),
                                fieldWithPath("permittables[].path").description("RequestMapping value"),
                                fieldWithPath("permittables[].method").type("RequestMethod").description("HTTP Request Method"),
                                fieldWithPath("permittables[].groupId").type("String").description("permittable identifier"),
                                fieldWithPath("permittables[].acceptTokenIntendedForForeignApplication").type(Boolean.TYPE).description("Accept token for foreign application")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentFindPGroup ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String identifier = testEnvironment.generateUniqueIdentifer("pgroup");

      final PermittableEndpoint permittableEndpoint = buildPermittableEndpoint();
      final PermittableGroup pgroup = buildPermittableGroup(identifier, permittableEndpoint);

      getTestSubject().createPermittableGroup(pgroup);

      {
        final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_PERMITTABLE_GROUP, pgroup.getIdentifier());
        Assert.assertTrue(found);
      }

      try {
        this.mockMvc.perform(get("/permittablegroups/" + pgroup.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-find-p-group", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("Permittable group identifier"),
                                fieldWithPath("permittables[].path").description("RequestMapping value"),
                                fieldWithPath("permittables[].method").type("RequestMethod").description("HTTP Request Method"),
                                fieldWithPath("permittables[].groupId").type("String").description("permittable identifier"),
                                fieldWithPath("permittables[].acceptTokenIntendedForForeignApplication").type(Boolean.TYPE).description("Accept token for foreign application ?")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentFindAllPGroups ( ) throws InterruptedException {

    try (final AutoUserContext ignore = loginAdmin()) {

      final String firstIdentifier = testEnvironment.generateUniqueIdentifer("pgroup");
      final String secondIdentifier = testEnvironment.generateUniqueIdentifer("pgroup");

      final List <String> identifierstrings = Lists.newArrayList(firstIdentifier, secondIdentifier);

      identifierstrings.stream()
              .forEach(string -> {

                final PermittableEndpoint permittableEndpoint = buildPermittableEndpoint();
                final PermittableGroup pGroup = buildPermittableGroup(string, permittableEndpoint);

                getTestSubject().createPermittableGroup(pGroup);

                {
                  final boolean found;
                  try {
                    found = eventRecorder.wait(EventConstants.OPERATION_POST_PERMITTABLE_GROUP, pGroup.getIdentifier());
                    Assert.assertTrue(found);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              });

      try {
        this.mockMvc.perform(get("/permittablegroups")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-find-all-p-groups", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].identifier").description("Permittable group identifier"),
                                fieldWithPath("[].permittables[].path").description("RequestMapping value"),
                                fieldWithPath("[].permittables[].method").type("RequestMethod").description("HTTP Request Method"),
                                fieldWithPath("[].permittables[].groupId").type("String").description("permittable identifier"),
                                fieldWithPath("[].permittables[].acceptTokenIntendedForForeignApplication").type(Boolean.TYPE).description("Accept token for foreign application ?"),
                                fieldWithPath("[1].identifier").description("Permittable group identifier"),
                                fieldWithPath("[1].permittables[].path").description("RequestMapping value"),
                                fieldWithPath("[1].permittables[].method").type("RequestMethod").description("HTTP Request Method"),
                                fieldWithPath("[1].permittables[].groupId").type("String").description("permittable identifier"),
                                fieldWithPath("[1].permittables[].acceptTokenIntendedForForeignApplication").type(Boolean.TYPE).description("Accept token for foreign application ?")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentCreateUser ( ) throws InterruptedException {

    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(username, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(username, userAuthentication.getAccessToken())) {

      UserWithPassword newUser = new UserWithPassword("Ahmes_friend_zero", "scribe_zero",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(post("/users")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serializer.toJson(newUser)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-user", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("user's identifier"),
                                fieldWithPath("role").description("user's role"),
                                fieldWithPath("password").description("user's password")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentFindUser ( ) throws InterruptedException {

    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(username, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(username, userAuthentication.getAccessToken())) {

      UserWithPassword newUser = new UserWithPassword("Ahmes_friend_three", "scribe_three",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));
      getTestSubject().createUser(newUser);
      final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_USER, newUser.getIdentifier());
      Assert.assertTrue(found);

      try {
        this.mockMvc.perform(get("/users/" + newUser.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-find-user", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("user's identifier"),
                                fieldWithPath("role").description("user's role")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentFindAllUsers ( ) throws InterruptedException {

    final String username = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(username, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(username, userAuthentication.getAccessToken())) {

      UserWithPassword ahmesFriend = new UserWithPassword("Ahmes_friend", "scribe",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));
      getTestSubject().createUser(ahmesFriend);
      final boolean found = eventRecorder.wait(EventConstants.OPERATION_POST_USER, ahmesFriend.getIdentifier());
      Assert.assertTrue(found);

      UserWithPassword ahmesOtherFriend = new UserWithPassword("Ahmes_Other_friend", "cobbler",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD + "Other"));
      getTestSubject().createUser(ahmesOtherFriend);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_USER, ahmesOtherFriend.getIdentifier()));

      try {
        this.mockMvc.perform(get("/users")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-find-all-users", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].identifier").description("first user's identifier"),
                                fieldWithPath("[].role").description("first user's role"),
                                fieldWithPath("[1].identifier").description("second user's identifier"),
                                fieldWithPath("[1].role").description("second user's role"),
                                fieldWithPath("[2].identifier").description("third user's identifier"),
                                fieldWithPath("[2].role").description("third user's role"),
                                fieldWithPath("[3].identifier").description("fourth user's identifier"),
                                fieldWithPath("[3].role").description("fourth user's role")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentChangeUserRole ( ) throws InterruptedException {

    final String userName = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(userName, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userName, userAuthentication.getAccessToken())) {

      UserWithPassword user = new UserWithPassword("Ahmes_friend_One", "scribe_one",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));
      getTestSubject().createUser(user);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_USER, user.getIdentifier()));

      RoleIdentifier newRole = new RoleIdentifier("cobbler");

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(put("/users/" + user.getIdentifier() + "/roleIdentifier")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serializer.toJson(newRole)))
                .andExpect(status().isAccepted())
                .andDo(document("document-change-user-role", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description(" updated role identifier")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetUserPermissions ( ) throws InterruptedException {

    final String userName = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(userName, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userName, userAuthentication.getAccessToken())) {

      UserWithPassword user = new UserWithPassword("Ahmes_friend_Two", "scribe_two",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));
      getTestSubject().createUser(user);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_USER, user.getIdentifier()));

      try {
        this.mockMvc.perform(get("/users/" + user.getIdentifier() + "/permissions")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-user-permissions", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].permittableEndpointGroupIdentifier").description(" permittable endpoint group identifier"),
                                fieldWithPath("[].allowedOperations").type("Set<AllowedOperation>").description("Set of allowed operations")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentChangeUserPassword ( ) throws InterruptedException {

    final String userName = createUserWithNonexpiredPassword(AHMES_PASSWORD, ADMIN_ROLE);

    final Authentication userAuthentication =
            getTestSubject().login(userName, TestEnvironment.encodePassword(AHMES_PASSWORD));

    try (final AutoUserContext ignored = new AutoUserContext(userName, userAuthentication.getAccessToken())) {

      UserWithPassword user = new UserWithPassword("Daddy", "bearer",
              TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD));
      getTestSubject().createUser(user);
      Assert.assertTrue(eventRecorder.wait(EventConstants.OPERATION_POST_USER, user.getIdentifier()));

      Password passw = new Password(TestEnvironment.encodePassword(AHMES_FRIENDS_PASSWORD + "Daddy"));

      user.setPassword(passw.getPassword());

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(put("/users/" + user.getIdentifier() + "/password")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serializer.toJson(passw)))
                .andExpect(status().isAccepted())
                .andDo(document("document-change-user-password", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("password").description("updated password")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetApplications ( ) throws InterruptedException {

    try (final AutoUserContext ignored = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData firstApplication = setApplicationSignature();
      final ApplicationSignatureTestData secondApplication = setApplicationSignature();

      try {
        this.mockMvc.perform(get("/applications")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-applications", preprocessResponse(prettyPrint())));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentDeleteApplication ( ) throws InterruptedException {

    try (final AutoUserContext ignored = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData firstApplication = setApplicationSignature();

      try {
        this.mockMvc.perform(delete("/applications/" + firstApplication.getApplicationIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-application", preprocessResponse(prettyPrint())));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentSetApplicationSignature ( ) throws InterruptedException {

    final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";

    final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();

    final String appTimeStamp = keyPair.getTimestamp();

    final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

    Gson serializer = new Gson();
    try {
      this.mockMvc.perform(put("/applications/" + appIdentifier + "/signatures/" + appTimeStamp)
              .accept(MediaType.APPLICATION_JSON_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(serializer.toJson(signature)))
              .andExpect(status().isAccepted())
              .andDo(document("document-set-application-signature", preprocessRequest(prettyPrint()),
                      requestFields(
                              fieldWithPath("publicKeyMod").type("BigInteger").description(" public key mod"),
                              fieldWithPath("publicKeyExp").type("BigInteger").description(" public key exp")
                      )
              ));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void documentGetApplicationSignature ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";

      final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();

      final String appTimeStamp = keyPair.getTimestamp();

      final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

      getTestSubject().setApplicationSignature(appIdentifier, appTimeStamp, signature);
      this.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, new ApplicationSignatureEvent(appIdentifier, keyPair.getTimestamp()));

      try {
        this.mockMvc.perform(get("/applications/" + appIdentifier + "/signatures/" + appTimeStamp)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.ALL_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-application-signature", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("publicKeyMod").type("BigInteger").description("public key mod"),
                                fieldWithPath("publicKeyExp").type("BigInteger").description("public key exp")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentCreateApplicationPermission ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {

      final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";

      final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();

      final String appTimeStamp = keyPair.getTimestamp();

      final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

      getTestSubject().setApplicationSignature(appIdentifier, appTimeStamp, signature);
      this.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, new ApplicationSignatureEvent(appIdentifier, keyPair.getTimestamp()));

      final Permission newPermission = new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, Sets.newHashSet(AllowedOperation.READ));

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(post("/applications/" + appIdentifier + "/permissions")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serializer.toJson(newPermission)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-application-permission", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("permittableEndpointGroupIdentifier").description("permittable group endpoint identifier"),
                                fieldWithPath("allowedOperations").type("Set<AllowedOperation>").description("Set of Allowed Operations")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetApplicationPermission ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {

      final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";

      final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();

      final String appTimeStamp = keyPair.getTimestamp();

      final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

      getTestSubject().setApplicationSignature(appIdentifier, appTimeStamp, signature);
      this.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, new ApplicationSignatureEvent(appIdentifier, keyPair.getTimestamp()));

      final Permission newPermission = new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, Sets.newHashSet(AllowedOperation.CHANGE));

      createApplicationPermission(appIdentifier, newPermission);

      Gson serializer = new Gson();
      try {
        this.mockMvc.perform(get("/applications/" + appIdentifier + "/permissions/" + newPermission.getPermittableEndpointGroupIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-application-permission", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("permittableEndpointGroupIdentifier").description("permittable group endpoint identifier"),
                                fieldWithPath("allowedOperations").type("Set<AllowedOperation>").description("Set of Allowed Operations")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetApplicationPermissions ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {

      final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";

      final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();

      final String appTimeStamp = keyPair.getTimestamp();

      final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

      getTestSubject().setApplicationSignature(appIdentifier, appTimeStamp, signature);
      this.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, new ApplicationSignatureEvent(appIdentifier, keyPair.getTimestamp()));

      final Permission firstPermission = new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, Sets.newHashSet(AllowedOperation.CHANGE));
      createApplicationPermission(appIdentifier, firstPermission);

      final Permission secondPermission = new Permission(PermittableGroupIds.ROLE_MANAGEMENT, Sets.newHashSet(AllowedOperation.DELETE));
      createApplicationPermission(appIdentifier, secondPermission);

      try {
        this.mockMvc.perform(get("/applications/" + appIdentifier + "/permissions")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-application-permissions", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].permittableEndpointGroupIdentifier").description("first permittable group endpoint identifier"),
                                fieldWithPath("[].allowedOperations").type("Set<AllowedOperation>").description("Set of Allowed Operations"),
                                fieldWithPath("[1].permittableEndpointGroupIdentifier").description("second permittable group endpoint identifier"),
                                fieldWithPath("[1].allowedOperations").type("Set<AllowedOperation>").description("Set of Allowed Operations")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentDeleteApplicationPermission ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {

      final String appIdentifier = "testApp" + RandomStringUtils.randomNumeric(3) + "-v1";
      final RsaKeyPairFactory.KeyPairHolder keyPair = RsaKeyPairFactory.createKeyPair();
      final String appTimeStamp = keyPair.getTimestamp();
      final Signature signature = new Signature(keyPair.getPublicKeyMod(), keyPair.getPublicKeyExp());

      getTestSubject().setApplicationSignature(appIdentifier, appTimeStamp, signature);
      this.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_SIGNATURE, new ApplicationSignatureEvent(appIdentifier, keyPair.getTimestamp()));

      final Permission newPermission = new Permission(PermittableGroupIds.IDENTITY_MANAGEMENT, Sets.newHashSet(AllowedOperation.CHANGE));
      createApplicationPermission(appIdentifier, newPermission);

      try {
        this.mockMvc.perform(delete("/applications/" + appIdentifier + "/permissions/" + newPermission.getPermittableEndpointGroupIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-application-permission", preprocessResponse(prettyPrint())));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentCreateApplicationCallEndpointSet ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData application = setApplicationSignature();

      final String endpointSetIdentifier = testEnvironment.generateUniqueIdentifer("end_pt_set");
      final CallEndpointSet endpointSet = new CallEndpointSet();
      endpointSet.setIdentifier(endpointSetIdentifier);
      endpointSet.setPermittableEndpointGroupIdentifiers(Collections.emptyList());

      Gson serial = new Gson();
      try {
        this.mockMvc.perform(post("/applications/" + application.getApplicationIdentifier() + "/callendpointset")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serial.toJson(endpointSet)))
                .andExpect(status().isAccepted())
                .andDo(document("document-create-application-call-endpoint-set", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("call endpoint set identifier"),
                                fieldWithPath("permittableEndpointGroupIdentifiers").type("List<String>").description("permittable group endpoint identifier")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentChangeApplicationCallEndpointSet ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData application = setApplicationSignature();

      final String endpointSetIdentifier = testEnvironment.generateUniqueIdentifer("end_pt_set");
      PermittableEndpoint pEndPointOne = buildPermittableEndpoint();
      PermittableEndpoint pEndPointTwo = buildPermittableEndpoint();

      PermittableGroup pgroup1 = buildPermittableGroup("ideeOne", pEndPointOne);
      PermittableGroup pgroup2 = buildPermittableGroup("ideeTwo", pEndPointTwo);

      List enlist = Lists.newArrayList(pgroup1.getIdentifier(), pgroup2.getIdentifier());

      final CallEndpointSet endpointSet = new CallEndpointSet();
      endpointSet.setIdentifier(endpointSetIdentifier);
      endpointSet.setPermittableEndpointGroupIdentifiers(enlist);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSet);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifier));

      Gson serial = new Gson();
      try {
        this.mockMvc.perform(put("/applications/" + application.getApplicationIdentifier() + "/callendpointset/" + endpointSet.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(serial.toJson(endpointSet)))
                .andExpect(status().isAccepted())
                .andDo(document("document-change-application-call-endpoint-set", preprocessRequest(prettyPrint()),
                        requestFields(
                                fieldWithPath("identifier").description("call endpoint set identifier"),
                                fieldWithPath("permittableEndpointGroupIdentifiers").type("List<String>").description("permittable group endpoint identifier")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetApplicationCallEndpointSets ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData application = setApplicationSignature();

      final String endpointSetIdentifierOne = testEnvironment.generateUniqueIdentifer("end_pt_set");
      final String endpointSetIdentifierTwo = testEnvironment.generateUniqueIdentifer("endptset");

      PermittableEndpoint pEndPointZero = buildPermittableEndpoint();
      PermittableEndpoint pEndPointOne = buildPermittableEndpoint();
      PermittableEndpoint pEndPointTwo = buildPermittableEndpoint();

      PermittableGroup pgroup = buildPermittableGroup("ideeZero", pEndPointZero);
      PermittableGroup pgroup1 = buildPermittableGroup("ideeOne", pEndPointOne);
      PermittableGroup pgroup2 = buildPermittableGroup("ideeTwo", pEndPointTwo);

      List enlist1 = Lists.newArrayList(pgroup1.getIdentifier(), pgroup2.getIdentifier());

      final CallEndpointSet endpointSetOne = new CallEndpointSet();
      endpointSetOne.setIdentifier(endpointSetIdentifierOne);
      endpointSetOne.setPermittableEndpointGroupIdentifiers(enlist1);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSetOne);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifierOne));

      List enlist2 = Lists.newArrayList(pgroup.getIdentifier());
      final CallEndpointSet endpointSetTwo = new CallEndpointSet();
      endpointSetTwo.setIdentifier(endpointSetIdentifierTwo);
      endpointSetTwo.setPermittableEndpointGroupIdentifiers(enlist2);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSetTwo);
      super.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifierTwo));

      Assert.assertTrue(getTestSubject().getApplicationCallEndpointSets(application.getApplicationIdentifier()).size() == 2);

      try {
        this.mockMvc.perform(get("/applications/" + application.getApplicationIdentifier() + "/callendpointset")
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-application-call-endpoint-sets", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].identifier").description("first call endpoint call set identifier"),
                                fieldWithPath("[].permittableEndpointGroupIdentifiers").type("List<String>").description("first permittable group endpoint identifier"),
                                fieldWithPath("[2].identifier").description("second call endpoint call set identifier"),
                                fieldWithPath("[2].permittableEndpointGroupIdentifiers").type("List<String>").description("second permittable group endpoint identifier")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentGetApplicationCallEndpointSet ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData application = setApplicationSignature();

      final String endpointSetIdentifierOne = testEnvironment.generateUniqueIdentifer("end_pt_set");
      final String endpointSetIdentifierTwo = testEnvironment.generateUniqueIdentifer("endptset");

      PermittableEndpoint pEndPointZero = buildPermittableEndpoint();
      PermittableEndpoint pEndPointOne = buildPermittableEndpoint();
      PermittableEndpoint pEndPointTwo = buildPermittableEndpoint();

      PermittableGroup pgroup = buildPermittableGroup("ideeZero", pEndPointZero);
      PermittableGroup pgroup1 = buildPermittableGroup("ideeOne", pEndPointOne);
      PermittableGroup pgroup2 = buildPermittableGroup("ideeTwo", pEndPointTwo);

      List enlist1 = Lists.newArrayList(pgroup1.getIdentifier(), pgroup2.getIdentifier());

      final CallEndpointSet endpointSetOne = new CallEndpointSet();
      endpointSetOne.setIdentifier(endpointSetIdentifierOne);
      endpointSetOne.setPermittableEndpointGroupIdentifiers(enlist1);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSetOne);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifierOne));

      List enlist2 = Lists.newArrayList(pgroup.getIdentifier());
      final CallEndpointSet endpointSetTwo = new CallEndpointSet();
      endpointSetTwo.setIdentifier(endpointSetIdentifierTwo);
      endpointSetTwo.setPermittableEndpointGroupIdentifiers(enlist2);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSetTwo);
      super.eventRecorder.wait(EventConstants.OPERATION_PUT_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifierTwo));

      Assert.assertTrue(getTestSubject().getApplicationCallEndpointSets(application.getApplicationIdentifier()).size() == 2);

      try {
        this.mockMvc.perform(get("/applications/" + application.getApplicationIdentifier() + "/callendpointset/" + endpointSetOne.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document("document-get-application-call-endpoint-set", preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("identifier").description("call endpoint call set identifier"),
                                fieldWithPath("permittableEndpointGroupIdentifiers").type("List<String>").description("permittable group endpoint identifier")
                        )
                ));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void documentDeleteApplicationCallEndpointSet ( ) throws InterruptedException {

    try (final AutoUserContext ignored
                 = tenantApplicationSecurityEnvironment.createAutoSeshatContext()) {
      final ApplicationSignatureTestData application = setApplicationSignature();

      final String endpointSetIdentifier = testEnvironment.generateUniqueIdentifer("end_pt_set");
      PermittableEndpoint pEndPointOne = buildPermittableEndpoint();
      PermittableEndpoint pEndPointTwo = buildPermittableEndpoint();

      PermittableGroup pgroup1 = buildPermittableGroup("yepue", pEndPointOne);
      PermittableGroup pgroup2 = buildPermittableGroup("yetah", pEndPointTwo);

      List enlist = Lists.newArrayList(pgroup1.getIdentifier(), pgroup2.getIdentifier());

      final CallEndpointSet endpointSet = new CallEndpointSet();
      endpointSet.setIdentifier(endpointSetIdentifier);
      endpointSet.setPermittableEndpointGroupIdentifiers(enlist);

      getTestSubject().createApplicationCallEndpointSet(application.getApplicationIdentifier(), endpointSet);
      super.eventRecorder.wait(EventConstants.OPERATION_POST_APPLICATION_CALLENDPOINTSET,
              new ApplicationCallEndpointSetEvent(application.getApplicationIdentifier(), endpointSetIdentifier));

      try {
        this.mockMvc.perform(delete("/applications/" + application.getApplicationIdentifier() + "/callendpointset/" + endpointSet.getIdentifier())
                .accept(MediaType.ALL_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isAccepted())
                .andDo(document("document-delete-application-call-endpoint-set", preprocessRequest(prettyPrint())));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private PermittableGroup buildPermittableGroup (final String identifier, final PermittableEndpoint permittableEndpoint) {
    final PermittableGroup ret = new PermittableGroup();
    ret.setIdentifier(identifier);
    ret.setPermittables(Collections.singletonList(permittableEndpoint));
    return ret;
  }

  private PermittableEndpoint buildPermittableEndpoint ( ) {
    final PermittableEndpoint ret = new PermittableEndpoint();
    ret.setPath("/exx/eyy/eze");
    ret.setMethod("POST");
    ret.setGroupId("id");
    return ret;
  }
}
