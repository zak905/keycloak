/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.requiredactions;

import java.io.IOException;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.Theme;
import org.keycloak.utils.UserHelper;

public class DeleteAccount implements RequiredActionProvider, RequiredActionFactory {

  public static final String PROVIDER_ID = "delete_account";

  private static final Logger logger = Logger.getLogger(DeleteAccount.class);

  @Override
  public String getDisplayText() {
    return "Delete Account";
  }

  @Override
  public void evaluateTriggers(RequiredActionContext context) {

  }

  @Override
  public void requiredActionChallenge(RequiredActionContext context) {
    UserModel user = context.getAuthenticationSession().getAuthenticatedUser();
    RealmModel realm = context.getRealm();

    if(!UserHelper.isDeleteAccountAllowed(realm, user)) {
      throw new ForbiddenException();
    }

    context.challenge(context.form().createForm("delete-account-confirm.ftl"));
  }


  @Override
  public void processAction(RequiredActionContext context) {
    KeycloakSession session = context.getSession();
    EventBuilder eventBuilder = context.getEvent();
    KeycloakContext keycloakContext = session.getContext();
    RealmModel realm = keycloakContext.getRealm();
    UserModel user = keycloakContext.getAuthenticationSession().getAuthenticatedUser();

    try {
      if(!UserHelper.isDeleteAccountAllowed(realm, user)) {
        throw new ForbiddenException();
      }
      boolean removed = new UserManager(session).removeUser(realm, user);

      if (removed) {
        eventBuilder.event(EventType.DELETE_ACCOUNT)
            .client(keycloakContext.getClient())
            .user(user)
            .detail(Details.USERNAME, user.getUsername())
            .success();
      } else {
        eventBuilder.event(EventType.DELETE_ACCOUNT)
            .client(keycloakContext.getClient())
            .user(user)
            .detail(Details.USERNAME, user.getUsername())
            .error("User could not be deleted");
        context.failure();
      }
    } catch (ForbiddenException forbidden) {
      logger.error("account client does not have the required roles for user deletion");
      eventBuilder.event(EventType.DELETE_ACCOUNT_ERROR)
          .client(keycloakContext.getClient())
          .user(keycloakContext.getAuthenticationSession().getAuthenticatedUser())
          .detail(Details.REASON, "does not have the required roles for user deletion")
          .error(Errors.USER_DELETE_ERROR);
      //deletingAccountForbidden
      context.challenge(context.form().setError(Messages.DELETE_ACCOUNT_LACK_PRIVILEDGES).createForm("delete-account-confirm.ftl"));
    } catch (Exception exception) {
      logger.error("unexpected error happened during account deletion", exception);
      eventBuilder.event(EventType.DELETE_ACCOUNT_ERROR)
          .client(keycloakContext.getClient())
          .user(keycloakContext.getAuthenticationSession().getAuthenticatedUser())
          .detail(Details.REASON, exception.getMessage())
          .error(Errors.USER_DELETE_ERROR);
      context.challenge(context.form().setError(Messages.DELETE_ACCOUNT_ERROR).createForm("delete-account-confirm.ftl"));
    }
  }



  @Override
  public RequiredActionProvider create(KeycloakSession session) {
    return this;
  }

  @Override
  public void init(Config.Scope config) {

  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {

  }

  @Override
  public void close() {

  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public InitiatedActionSupport initiatedActionSupport() {
    return InitiatedActionSupport.SUPPORTED;
  }

  @Override
  public boolean isOneTimeAction() {
    return true;
  }
}