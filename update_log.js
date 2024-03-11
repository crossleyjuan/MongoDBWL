var agg = [
  { $match: { "msg": "Slow query" } },
  {
    $group:
      /**
       * _id: The id of the group.
       * fieldN: The first field name.
       */
      {
        _id: "$ctx",
        maxDate: {
          $max: "$t",
        },
        minDate: {
          $min: "$t",
        },
      },
  },
  {
    $merge:
      /**
       * into: The target collection.
       * on: Fields to  identify.
       * let: Defined variables.
       * whenMatched: Action for matching docs.
       * whenNotMatched: Action for non-matching docs.
       */
      {
        into: "contexts",
        on: "_id",
        whenMatched: "keepExisting",
      },
  },
];

//db.getSiblingDB("mongoDBAnalysis").contexts.drop();
db.getSiblingDB("mongoDBAnalysis").logs.createIndex({ "msg": 1, "ctx": 1, "t": 1 });
db.getSiblingDB("mongoDBAnalysis").logs.createIndex({ "ctx": 1, "t": 1 });
db.getSiblingDB("mongoDBAnalysis").logs.aggregate(agg);

