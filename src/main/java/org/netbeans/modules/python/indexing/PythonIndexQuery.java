package org.netbeans.modules.python.indexing;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonIndexQuery {

    static public void queryPerFile(URL root, FileObject file, String key) {
        try {
            QuerySupport.Query fileQuery = QuerySupport
                    .forRoots("PythonIndexer", 0, root)
                    .getQueryFactory().file(file);
            fileQuery.execute(key);

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    public static Collection<? extends IndexResult> searchIndex(URL toURL, String key, SearchType searchType, String text) {
        try {
            QuerySupport forRoots = QuerySupport.forRoots("PythonIndexer", 0, toURL);
            QuerySupport.Query.Factory queryFactory = forRoots.getQueryFactory();

            //Dont understand how these stupid Lucene queries works
            //            QuerySupport.Query query = null;
            //            System.out.println("REQUEST TYPE: " + searchType);
            //            switch (searchType.toString()) {
            //                case "PREFIX":
            //                    //PREFIX
            //                    query = queryFactory.field(key, text, QuerySupport.Kind.PREFIX);
            //                    System.out.println("PREFIX");
            //                    break;
            //                case "CASE_INSENSITIVE_PREFIX":
            //                    //CASE_INSENSITIVE_PREFIX XXX
            //                    query = queryFactory.field(key, text, QuerySupport.Kind.CASE_INSENSITIVE_PREFIX);
            //                    System.out.println("CASE_INSENSITIVE_PREFIX");
            //                    break;
            //                case "EXACT":
            //                    query = queryFactory.field(key, text, QuerySupport.Kind.EXACT);
            //                    System.out.println("EXACT");
            //                    break;
            //                case "REGEXP":
            //                    //REGEXP
            //                    query = queryFactory.field(key, fixRegex(text), QuerySupport.Kind.REGEXP);
            //                    System.out.println("REGEXP");
            //                    break;
            //                case "CASE_INSENSITIVE_REGEXP":
            //                    //CASE_INSENSITIVE_REGEXP
            //                    query = queryFactory.field(key, fixRegex(text), QuerySupport.Kind.CASE_INSENSITIVE_REGEXP);
            //                    System.out.println("CASE_INSENSITIVE_REGEXP");
            //                    break;
            //                case "CASE_INSENSITIVE_CAMEL_CASE"://good
            ////                    query = queryFactory.field(key, text, QuerySupport.Kind.CASE_INSENSITIVE_CAMEL_CASE);
            ////                    System.out.println("CASE_INSENSITIVE_CAMEL_CASE");
            ////                    break;
            //                case "CAMEL_CASE"://good works with sensitive enablefor StreamHandlerRequest/ SHR
            //                    query = queryFactory.field(key, text, QuerySupport.Kind.CAMEL_CASE);
            //                    System.out.println("CAMEL_CASE");
            //                    break;
            ////                    query = queryFactory.or(exactValue, camelCaseValue, cICamelValue);
            ////                    System.out.println("DEFAULT");
            //                ///.*work.*/i
            //            }
            //PREFIX
            //QuerySupport.Query prefixValue = queryFactory.field(key, text, QuerySupport.Kind.PREFIX);
            ////CASE_INSENSITIVE_PREFIX
            //QuerySupport.Query cIPValue = queryFactory.field(key, text, QuerySupport.Kind.CASE_INSENSITIVE_PREFIX);
            ////REGEXP fixRegex
            //QuerySupport.Query regexValue = queryFactory.field(key, fixRegex(text), QuerySupport.Kind.REGEXP);
            ////CASE_INSENSITIVE_REGEXP fixRegex
            //QuerySupport.Query cIRValue = queryFactory.field(key, fixRegex(text), QuerySupport.Kind.CASE_INSENSITIVE_REGEXP);
            //          //QuerySupport.Query exactValue = queryFactory.field(key, text, QuerySupport.Kind.EXACT);
            //QuerySupport.Query camelCaseValue = queryFactory.field(key, text, QuerySupport.Kind.CAMEL_CASE);
            //QuerySupport.Query cICamelValue = queryFactory.field(key, text, QuerySupport.Kind.CASE_INSENSITIVE_CAMEL_CASE);
            //QuerySupport.Query query = queryFactory.or(exactValue, camelCaseValue, cICamelValue, cIPValue, cIRValue, prefixValue, regexValue);
            //return query.execute(key);
            //
            //so returning everything! seems filtering is performed on the Go to panel
            return queryFactory.field(key, ".*", QuerySupport.Kind.REGEXP).execute(key);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public static String fixRegex(String text) {
        if (StringUtils.startsWithAny(text, new String[]{"?", "+", "*", "{", "[", "(", ")"})) {
            return "." + text;
        }
        return text;

    }

}
