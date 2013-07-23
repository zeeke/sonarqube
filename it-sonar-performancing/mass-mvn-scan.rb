# -------------------------
# CONFIGURATION
# -------------------------

# Number of paths should not be greater than number of cpus
maven_projects = [
  '/Users/sbrandhof/projects/commons-io',
  '/Users/sbrandhof/projects/http-request/lib',
  '/Users/sbrandhof/projects/sonarsource/sonar-tests-core/it-sonar/tests/projects/shared/sample'
]

# DB settings
db_url='jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true'
db_login='sonar'
db_password='sonar'


# Number of inspections per project.
# Each inspection uses a new branch so total nb of inspections = paths.size * branches_count
branches_count = 100


# -------------------------
# DO NOT EDIT THE FOLLOWING
# -------------------------
mvn_parameters = "-Dsonar.dynamicAnalysis=false -Dsonar.jdbc.url='#{db_url}' -Dsonar.jdbc.username=#{db_login} -Dsonar.jdbc.password=#{db_password}"
threads = []
for path in maven_projects
  threads << Thread.new(path) do |project_path|
    for index in 1..branches_count
      puts "------------- #{index}/#{branches_count} of #{project_path}\n"
      command = "mvn sonar:sonar -Dsonar.branch=branch_#{index} -Dsonar.cpd.cross_project=false #{mvn_parameters} -f #{project_path}/pom.xml"
      system(command)
    end
  end
end

threads.each do |t|
  t.join
end
