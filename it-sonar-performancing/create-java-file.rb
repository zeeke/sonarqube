# HOW TO USE 
# In order to create the file Foo.java around 10'000 lines long :
# ruby create-java-file.rb Foo 10000


if ARGV.size!=2
  puts "Missing command arguments: <classname> <lines>"
  exit 1
end
classname = ARGV[0]
filename = "#{classname}.java"
lines = ARGV[1].to_i
puts "Generating file #{filename} with approximatively #{lines} physical lines"

File.open(filename, 'w') do |file| 
  METHOD_LINES=6
  file.puts("public class #{classname} {") 
  for index in 0..(lines/METHOD_LINES) do 
    file.puts("  public void do#{index}(int unused) {")
    file.puts("    if (true) {")
    file.puts('      System.out.println("Hello");  ')
    file.puts("    }")
    file.puts("  }")
    file.puts("  ")
  end
  file.puts("}") 
end