servers=( "34.242.75.105" "34.245.92.139" "34.250.46.169" "52.213.157.161" "34.253.224.149" "52.19.49.69" "34.243.176.198" "34.254.249.56" "34.249.129.197" "34.245.57.86" )

for server in ${servers[@]}; do
  echo "$server"
  #scp loadTestErmetic_20231222.jar run.sh ec2-user@$server:.
  rsync -avh loadTestErmetic_20231222.jar ec2-user@$server:.
  rsync -avh run.sh ec2-user@$server:.
done
for server in ${servers[@]}; do
  ssh ec2-user@$server "sudo yum -y install java"
done
