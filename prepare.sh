source config.sh

echo "$servers"
for server in ${servers[@]}; do
  echo "$server"
  #ssh ec2-user@$server -o ServerAliveInterval=30  "sudo killall java"
  scp loadTestMongoDB_20231222.jar run.sh ec2-user@$server:.
  rsync -avh loadTestMongoDB_20231222.jar ec2-user@$server:.
  rsync -avh run.sh ec2-user@$server:.
  rsync -avh contexts_to_force.txt ec2-user@$server:.
done
for server in ${servers[@]}; do
  ssh ec2-user@$server "sudo yum -y install java"
done
