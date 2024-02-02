var agg = [
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

db.getSiblingDB("mongoDBAnalysis").logs.aggregate(agg);

