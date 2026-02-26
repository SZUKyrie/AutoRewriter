package org.autorewriter.common.utils;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;

import static org.autorewriter.common.constant.NotationConstants.DOT;
/**
 * Utils for table name
 */
public class TableNameUtils {

    public static String getQualifiedNameString(List<String> parents, String tableName) {
        List<String> qualifiedName = getQualifiedName(parents, tableName);
        return getQualifiedNameString(qualifiedName);
    }

    public static String getQualifiedNameString(List<String> qualifiedTableName) {
        String fullNameString = String.join(DOT, qualifiedTableName);
        return fullNameString;
    }

    public static List<String> getQualifiedName(List<String> parents, String tableName) {
        List<String> names = new ArrayList<>(parents);
        names.add(tableName);
        return names;
    }

    public static List<String> getQualifiedNameList(String qualifiedTableName) {
        List<String> qualifiedNameList = Splitter.on(DOT).omitEmptyStrings().splitToList(qualifiedTableName);
        return qualifiedNameList;
    }

}
