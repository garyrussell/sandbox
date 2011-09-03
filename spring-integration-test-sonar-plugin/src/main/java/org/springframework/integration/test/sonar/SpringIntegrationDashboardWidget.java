package org.springframework.integration.test.sonar;

import org.sonar.api.Extension;
import org.sonar.api.web.*;

@UserRole(UserRole.USER)
@Description("SI Channel Coverage")
public class SpringIntegrationDashboardWidget extends AbstractRubyTemplate implements RubyRailsWidget, Extension {

  public String getId() {
    return "channelcoverage";
  }

  public String getTitle() {
    return "Channel Coverage";
  }

  @Override
  protected String getTemplatePath() {
    return "/channel_coverage_dashboard_widget.html.erb";
  }
}