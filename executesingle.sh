function usage() {
	echo ".sh -t <test> -s <server>"
	echo ""
	echo "-t n (normal),nl (normal with contexts),m (merged db), ml (merged with lastcontexts)" 
	echo "-s 0,1,2,3,4 server"
	echo ""
	echo "Example:"
	echo "./executesingle.sh -t n -s 0"
}

while getopts "t:s:" o; do
    case "${o}" in
        t)
            TEST=${OPTARG}
            ;;
        s)
            SERVER=${OPTARG}
            ;;
        *)
            usage
            ;;
    esac
done
shift $((OPTIND-1))

source config.sh

server=${servers[$SERVER]}

echo $server
ssh ec2-user@$server -o ServerAliveInterval=30  "chmod +x run.sh;./run.sh -t ${TEST}"
