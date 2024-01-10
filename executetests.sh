servers=( "34.242.75.105" "34.245.92.139" "34.250.46.169" "52.213.157.161" "34.253.224.149" "52.19.49.69" "34.243.176.198" "34.254.249.56" "34.249.129.197" "34.245.57.86" )

for server in ${servers[@]}; do
  (echo "output from $server"; ssh ec2-user@$server "chmod +x run.sh;./run.sh"; echo End $server) &
done
wait
echo All subshells finished
