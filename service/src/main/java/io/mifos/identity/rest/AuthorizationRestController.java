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
package io.mifos.identity.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.anubis.api.v1.TokenConstants;
import io.mifos.anubis.security.AmitAuthenticationException;
import io.mifos.core.command.domain.CommandCallback;
import io.mifos.core.command.domain.CommandProcessingException;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import io.mifos.identity.api.v1.PermittableGroupIds;
import io.mifos.identity.api.v1.client.IdentityManager;
import io.mifos.identity.api.v1.domain.Authentication;
import io.mifos.identity.internal.command.AuthenticationCommandResponse;
import io.mifos.identity.internal.command.PasswordAuthenticationCommand;
import io.mifos.identity.internal.command.RefreshTokenAuthenticationCommand;
import io.mifos.identity.internal.util.IdentityConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutionException;


/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@RestController //
public class AuthorizationRestController {

  private final CommandGateway commandGateway;
  private final Logger logger;

  //Whether the cookie can only be transported via https.  Should only be set to false for testing.
  @Value("${identity.token.refresh.secureCookie:true}")
  private boolean secureRefreshTokenCookie;
  @Value("${server.contextPath}")
  private String contextPath;

  @Autowired public AuthorizationRestController(
          final CommandGateway commandGateway,
          @Qualifier(IdentityConstants.LOGGER_NAME) final Logger logger) {
    super();
    this.commandGateway = commandGateway;
    this.logger = logger;
  }

  @RequestMapping(
      value = "/token",
      method = RequestMethod.POST,
      consumes = {MediaType.ALL_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  @Permittable(AcceptedTokenType.GUEST)
  public
  @ResponseBody ResponseEntity<Authentication> authenticate(
          final HttpServletResponse response,
          final HttpServletRequest request,
          @RequestParam("grant_type") final String grantType,
          @RequestParam(value = "username", required = false) final String username,
          @RequestParam(value = "password", required = false) final String password,
          @RequestHeader(value = IdentityManager.REFRESH_TOKEN, required = false) final String refreshTokenParam) throws InterruptedException {
    switch (grantType) {
      case "refresh_token": {
        final String refreshToken = getRefreshToken(refreshTokenParam, request);

        try {
          final AuthenticationCommandResponse authenticationCommandResponse
                  = getAuthenticationCommandResponse(new RefreshTokenAuthenticationCommand(refreshToken));
          final Authentication ret = map(authenticationCommandResponse, response);

          return new ResponseEntity<>(ret, HttpStatus.OK);
        }
        catch (final AmitAuthenticationException e)
        {
          return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
      }
      case "password": {
        if (username == null)
          throw ServiceException.badRequest("The query parameter username must be set if the grant_type is password.");
        if (password == null)
          throw ServiceException.badRequest("The query parameter password must be set if the grant_type is password.");

        try {
          final Authentication ret = map(getAuthenticationCommandResponse(
              new PasswordAuthenticationCommand(username, password)), response);
          return new ResponseEntity<>(ret, HttpStatus.OK);
        }
        catch (final AmitAuthenticationException e)
        {
          return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

      }
      default:
        throw ServiceException.badRequest("invalid grant type: " + grantType);
    }
  }

  @RequestMapping(value = "/token/_current", method = RequestMethod.DELETE,
          consumes = {MediaType.ALL_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.SELF_MANAGEMENT)
  @ResponseBody ResponseEntity<Void> logout(
          final HttpServletResponse response,
          final HttpServletRequest request)
  {
    response.addCookie(bakeRefreshTokenCookie(""));

    return ResponseEntity.ok().build();
  }

  private String getRefreshToken(final @Nullable String refreshTokenParam, final HttpServletRequest request) {
    if (refreshTokenParam != null)
      return refreshTokenParam;

    final Cookie refreshTokenCookie = WebUtils.getCookie(request, TokenConstants.REFRESH_TOKEN_COOKIE_NAME);
    if (refreshTokenCookie == null)
      throw ServiceException.badRequest("One (and only one) refresh token cookie must be included in the request if the grant_type is refresh_token");

    return refreshTokenCookie.getValue();
  }

  private AuthenticationCommandResponse getAuthenticationCommandResponse(
      final Object authenticationCommand) throws AmitAuthenticationException, InterruptedException {
    try
    {
      final CommandCallback<AuthenticationCommandResponse> ret =
          commandGateway.process(authenticationCommand,
                  AuthenticationCommandResponse.class);

      return ret.get();
    }
    catch (final ExecutionException e)
    {
      if (AmitAuthenticationException.class.isAssignableFrom(e.getCause().getClass()))
      {
        logger.debug("Authentication failed.", e);
        throw AmitAuthenticationException.class.cast(e.getCause());
      }
      else if (CommandProcessingException.class.isAssignableFrom(e.getCause().getClass()))
      {
        final CommandProcessingException commandProcessingException = (CommandProcessingException) e.getCause();
        if (ServiceException.class.isAssignableFrom(commandProcessingException.getCause().getClass()))
          throw (ServiceException)commandProcessingException.getCause();
        else {
          logger.error("Authentication failed with an unexpected error.", e);
          throw ServiceException.internalError("An error occurred while attempting to authenticate a user.");
        }
      }
      else if (ServiceException.class.isAssignableFrom(e.getCause().getClass()))
      {
        throw (ServiceException)e.getCause();
      }
      else {
        logger.error("Authentication failed with an unexpected error.", e);
        throw ServiceException.internalError("An error occurred while attempting to authenticate a user.");
      }
    }
    catch (final CommandProcessingException e)
    {
      logger.error("Authentication failed with an unexpected error.", e);
      throw ServiceException.internalError("An error occurred while attempting to authenticate a user.");
    }
  }

  private Authentication map(
          final AuthenticationCommandResponse commandResponse,
          final HttpServletResponse httpServletResponse)
  {
    httpServletResponse.addCookie(bakeRefreshTokenCookie(commandResponse.getRefreshToken()));

    return new Authentication(
            commandResponse.getAccessToken(),
            commandResponse.getAccessTokenExpiration(),
            commandResponse.getRefreshTokenExpiration(),
            commandResponse.getPasswordExpiration());
  }

  private Cookie bakeRefreshTokenCookie(final String refreshToken) {
    final Cookie refreshTokenCookie = new Cookie(TokenConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    refreshTokenCookie.setSecure(secureRefreshTokenCookie);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setPath(contextPath + "/token");
    return refreshTokenCookie;
  }
}
