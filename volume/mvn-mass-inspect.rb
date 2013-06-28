# CONFIGURATION

# Number of paths should not be greater than number of cpus
paths = [
  '/Users/sbrandhof/projects/commons-i18n',
  '/Users/sbrandhof/projects/commons-dbcp'
]

# Number of inspections per project.
# Each inspection uses a new branch so total nb of inspections = paths.size * branches_count
branches_count = 10000

mvn_parameters = "-Dsonar.dynamicAnalysis=false -Dsonar.jdbc.url=jdbc:oracle:thin:@172.16.199.142/XE"



# DO NOT EDIT THE FOLLOWING
threads = []
for path in paths
  threads << Thread.new(path) do |project_path|
    Dir.chdir(project_path) do
      for index in 1..branches_count
        puts "------------- #{index}/#{branches_count} of #{project_path}\n"
        command = "mvn sonar:sonar -Dsonar.branch=branch_#{index} -Dsonar.cpd.cross_project=false #{mvn_parameters}"
        system(command)
      end
    end
  end
end

threads.each do |t|
  t.join
end
