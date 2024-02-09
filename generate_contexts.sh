#!/bin/bash

logs_path="$1"
#
for log_file in "$logs_path"/*.log; do
  if [ -f "$log_file" ] && [ -r "$log_file" ]; then
	cat ${log_file}|grep TypeName|jq -r ". | select( .attr.docsExamined > 10000) | .ctx"
  fi
done

#cat atlas-1254uk-shard-00-02.xlruw.mongodb.net_2023-12-12T19_30_00_2023-12-12T22_30_00_mongodb.log|grep TypeName|jq -r ". | select( .attr.docsExamined > 100000) | .ctx" >> ~/workspace/mongodb/customers/ermetic/loadTestErmetic_20231222/contexts_to_force.txt
