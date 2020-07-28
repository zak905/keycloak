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

package org.keycloak.utils;

import java.util.Objects;
import java.util.Set;

import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

public class UserHelper {

  public static boolean hasAccountClientDeleteAccountRole(UserModel user, ClientModel accountClientModel) {
    Set<RoleModel> acountClientRoleMappings = user.getClientRoleMappings(accountClientModel);
    return Objects.nonNull(acountClientRoleMappings) && acountClientRoleMappings.stream().anyMatch((role) -> Objects.equals(role.getName(),
        AccountRoles.DELETE_ACCOUNT));
  }

  public static boolean isDeleteAccountAllowed(RealmModel realm, UserModel user) {
    RequiredActionProviderModel deleteAction = realm.getRequiredActionProviderByAlias("delete_account");
    return hasAccountClientDeleteAccountRole(user, realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)) && Objects.nonNull(deleteAction) && deleteAction.isEnabled();
  }

}