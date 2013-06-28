#
# Copyright (C) 2009-2013 SonarSource SA
# All rights reserved
# mailto:contact AT sonarsource DOT com
#

class ResourceConfigurationController < ApplicationController
  SECTION=Navigation::SECTION_RESOURCE

  def index
    init_resource_for_role(:user, :resource) if params[:resource]
  end

end
