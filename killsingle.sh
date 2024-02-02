source config.sh

server=${servers[$1]}

ssh ec2-user@$server -o ServerAliveInterval=30  "sudo killall java"

