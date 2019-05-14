
//Condition functions

function exists(document, fieldName) {
    return document.getField(fieldName).hasValues();
}

function stringIs(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.equalsIgnoreCase(expectedValue)) {
            return true;
        }
    });
}

function stringContains(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
            .contains(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function stringStartsWith(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
            .startsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function stringEndsWith(document, fieldName, value) {
    return evaluateValuesAgainstCondition(document, fieldName, value, function (expectedValue, actualValue) {
        if (actualValue.toUpperCase(java.util.Locale.getDefault())
            .endsWith(expectedValue.toUpperCase(java.util.Locale.getDefault()))) {
            return true;
        }
    });
}

function regexCondition(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Regex is not supported");
}

function dateBefore(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date before is not supported");
}

function dateAfter(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date after is not supported");
}

function dateOn(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Date on is not supported");
}

function numberGt(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number greater than is not supported");
}

function numberLt(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number less than is not supported");
}

function numberEq(document, fieldName, value) {
    throw new java.lang.UnsupportedOperationException("Number equal to is not supported");
}

function not(aBoolean) {
    return !aBoolean;
}
