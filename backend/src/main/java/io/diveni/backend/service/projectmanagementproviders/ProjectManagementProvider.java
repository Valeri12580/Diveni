/*
  SPDX-License-Identifier: AGPL-3.0-or-later
  Diveni - The Planing-Poker App
  Copyright (C) 2022 Diveni Team, AUME-Team 21/22, HTWG Konstanz
*/
package io.diveni.backend.service.projectmanagementproviders;

import java.util.List;
import java.util.Map;

import io.diveni.backend.model.Project;
import io.diveni.backend.model.UserStory;

public interface ProjectManagementProvider {
  boolean serviceEnabled();

  List<Project> getProjects(String tokenIdentifier, Map<String, String[]> reqParams);

  List<String>getSites(String tokenIdentifier);

  List<UserStory> getIssues(String tokenIdentifier, String projectName);

  void updateIssue(String tokenIdentifier, UserStory story);

  String createIssue(String tokenIdentifier, String projectID, UserStory story);

  void deleteIssue(String tokenIdentifier, String issueID);

  boolean containsToken(String token);

  String getCurrentUsername(String tokenIdentifier);
}
