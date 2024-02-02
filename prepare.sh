source config.sh

for server in ${servers[@]}; do
  echo "$server"
  #scp loadTestErmetic_20231222.jar run.sh ec2-user@$server:.
  rsync -avh loadTestErmetic_20231222.jar ec2-user@$server:.
  rsync -avh run.sh ec2-user@$server:.
  rsync -avh contexts_to_force.txt ec2-user@$server:.
done
for server in ${servers[@]}; do
  ssh ec2-user@$server "sudo yum -y install java"
done
