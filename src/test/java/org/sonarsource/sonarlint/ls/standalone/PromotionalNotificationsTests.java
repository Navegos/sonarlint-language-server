/*
 * SonarLint Language Server
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls.standalone;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;
import org.sonarsource.sonarlint.ls.SonarLintExtendedLanguageClient;
import org.sonarsource.sonarlint.ls.standalone.notifications.PromotionalNotifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PromotionalNotificationsTests {
  private final SonarLintExtendedLanguageClient client = mock(SonarLintExtendedLanguageClient.class);
  private final PromotionalNotifications underTest = new PromotionalNotifications(client);
  ArgumentCaptor<List<String>> promotedLanguagesCaptor = ArgumentCaptor.forClass(List.class);

  @Test
  void shouldSendNotification_notConnected_commercialLanguage() {
    underTest.promoteExtraEnabledLanguagesInConnectedMode(Set.of(Language.COBOL));

    verify(client).maybeShowWiderLanguageSupportNotification(List.of("COBOL"));
  }

  @Test
  void shouldSendNotification_notConnected_sql() {
    underTest.promoteExtraEnabledLanguagesInConnectedMode(Set.of(Language.PLSQL, Language.TSQL));

    verify(client).maybeShowWiderLanguageSupportNotification(promotedLanguagesCaptor.capture());
    List<String> promotedLanguages = promotedLanguagesCaptor.getValue();
    assertThat(promotedLanguages).containsExactlyInAnyOrder("PL/SQL", "T-SQL");
  }

  @Test
  void shouldSendNotification_notConnected_oraclesql() {
    underTest.promoteExtraEnabledLanguagesInConnectedMode(Set.of(Language.PLSQL));

    verify(client).maybeShowWiderLanguageSupportNotification(List.of("PL/SQL"));
  }
}
