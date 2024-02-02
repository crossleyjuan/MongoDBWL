source config.sh

for server in ${servers[@]}; do
  (echo "output from $server"; ssh ec2-user@$server "chmod +x run.sh;./run.sh"; echo End $server) &
done
wait
echo All subshells finished
