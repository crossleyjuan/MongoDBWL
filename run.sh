#java -jar loadTestErmetic_20231222.jar -u "mongodb+srv://juancrossley:JpnNqSFKnD7sokWS@ermetic.mpxmc.mongodb.net/?retryWrites=true" -t 40 -d 110000 -c 20000 -i "contexts_to_force.txt"
export URI='mongodb+srv://juancrossley:JpnNqSFKnD7sokWS@ermeticb.mpxmc.mongodb.net/?retryWrites=true&w=majority'
java -jar loadTestErmetic_20231222.jar -u "${URI}" -t 40 -d 110000 -c 20000  -i "contexts_to_force.txt" -p true -f false -q false -a false



