package com.leezg.app.nmerodiary.others

class SQLiteHelper {

    companion object {

        // Compose SQL string by using passed in parameters
        fun composeSQLString(
            tableName: String,
            text: String,
            orderBy: String,
            sequence: Int
        ): String {
            var searchText = text

            // If search text contains single quote, then handle it by adding escape char to avoid crashing
            if (searchText.indexOf("'") > -1) {
                for ((i, escapeChar) in searchText.toCharArray().withIndex()) {
                    if (escapeChar.toString() == "'")
                        searchText = StringBuilder(searchText).insert(i.plus(1), "'").toString()
                }
            }

            val finalSearchText = "'%$searchText%'"
            val sqlString1 =
                "SELECT * FROM $tableName " + if (tableName == Constant.Record) "WHERE ${Constant.isTemplate} = 0 " else "WHERE 1 = 1 "
            val sqlString2 =
                if (searchText.isNotBlank()) "AND ${Constant.searchString} LIKE $finalSearchText " else ""
            val sqlString3 =
                "ORDER BY ${Constant.isPinned} DESC, ${if (orderBy.isNotBlank()) orderBy else defaultOrderByField(
                    tableName
                )} "
            val sqlString4 = if (sequence == Constant.ASCENDING) "ASC" else "DESC"
            return sqlString1 + sqlString2 + sqlString3 + sqlString4
        }

        // Compose SQL string by using passed in parameters
        fun composeSQLString(
            tableName: String,
            columnNamesList: MutableList<String>,
            conditionOperatorsList: MutableList<Int>,
            valuesList: MutableList<MutableList<String>>
        ): String {
            val sqlString1 = "SELECT DISTINCT $tableName.* FROM $tableName "
            val sqlString2 =
                if (tableName == Constant.Record) "LEFT JOIN Field ON Field.${Constant.identifier} = $tableName.${Constant.identifier} WHERE 1 = 1 " else ""
            var sqlString3 = ""
            columnNamesList.forEachIndexed { index, columnName ->
                val conditionOperatorIndex = conditionOperatorsList[index]

                sqlString3 +=
                    when {
                        (columnName in Constant.standardFields) -> "AND LOWER($tableName.$columnName) ${getConditionString(
                            conditionOperatorIndex, valuesList[index]
                        )} "
                        columnName == Constant.fieldUnit -> "AND LOWER(Field.${Constant.fieldUnit}) ${getConditionString(
                            conditionOperatorIndex, valuesList[index]
                        )} "
                        columnName == Constant.fieldName -> "AND LOWER(Field.${Constant.fieldName}) ${getConditionString(
                            conditionOperatorIndex, valuesList[index]
                        )} "
                        else -> "AND Field.${Constant.fieldName} = '$columnName' AND LOWER(Field.${Constant.fieldValue}) ${getConditionString(
                            conditionOperatorIndex, valuesList[index]
                        )} "
                    }
            }

            return sqlString1 + sqlString2 + sqlString3
        }

        private fun getConditionString(
            conditionOperatorIndex: Int,
            valuesList: MutableList<String>
        ): String {
            return when (conditionOperatorIndex) {
                0 -> "${Constant.Is} LOWER('${valuesList[0]}') "
                1 -> {
                    var inString = ""
                    valuesList.forEachIndexed { index, value ->
                        inString += "'$value'"

                        if (index < valuesList.size.minus(1))
                            inString += ", "
                    }
                    "${Constant.Are} ($inString)"
                }
                2 -> "${Constant.Between} LOWER('${valuesList[0]}') AND LOWER('${valuesList[1]}') "
                3 -> "${Constant.NewerThan} LOWER('${valuesList[0]}') "
                4 -> "${Constant.OlderThan} LOWER('${valuesList[0]}') "
                5 -> "${Constant.LargerThan} LOWER('${valuesList[0]}') "
                6 -> "${Constant.SmallerThan} LOWER('${valuesList[0]}') "
                7 -> "${Constant.LargerOrEqualTo} LOWER('${valuesList[0]}') "
                8 -> "${Constant.SmallerOrEqualTo} LOWER('${valuesList[0]}') "
                else -> "${Constant.Contains} LOWER('%${valuesList[0]}%') "
            }
        }

        // Get default order by column
        private fun defaultOrderByField(tableName: String): String {
            return when (tableName) {
                Constant.Record -> Constant.recordTitle
                Constant.Folder -> Constant.folderName
                else -> ""
            }
        }
    }
}