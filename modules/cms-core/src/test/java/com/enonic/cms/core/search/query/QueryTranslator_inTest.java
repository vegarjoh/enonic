/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.query;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

import com.enonic.cms.core.content.index.ContentIndexQuery;

public class QueryTranslator_inTest
    extends QueryTranslatorTestBase
{
    @Test
    public void testIn_string()
        throws Exception
    {
        String expected_search_result = "{\n" +
            "  \"from\" : 0,\n" +
            "  \"size\" : " + ContentIndexQuery.DEFAULT_COUNT + ",\n" +
            "  \"query\" : {\n" +
            "    \"filtered\" : {\n" +
            "      \"query\" : {\n" +
            "        \"bool\" : {\n" +
            "          \"should\" : [ {\n" +
            "            \"term\" : {\n" +
            "              \"title\" : \"hello\"\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"title\" : \"test 2\"\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"title\" : \"my testcontent\"\n" +
            "            }\n" +
            "          } ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ContentIndexQuery query = createContentQuery( "title IN (\"Hello\", \"Test 2\", \"my testcontent\")" );

        SearchSourceBuilder builder = getQueryTranslator().build( query );

        compareStringsIgnoreFormatting( expected_search_result, builder.toString() );
    }

    @Test
    public void testIn_int()
        throws Exception
    {
        String expected_search_result = "{\n" +
            "  \"from\" : 0,\n" +
            "  \"size\" : " + ContentIndexQuery.DEFAULT_COUNT + ",\n" +
            "  \"query\" : {\n" +
            "    \"filtered\" : {\n" +
            "      \"query\" : {\n" +
            "        \"bool\" : {\n" +
            "          \"should\" : [ {\n" +
            "            \"term\" : {\n" +
            "              \"myintfield.number\" : 1.0\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"myintfield.number\" : 2.0\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"myintfield.number\" : 3.0\n" +
            "            }\n" +
            "          } ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ContentIndexQuery query = createContentQuery( "myIntField IN (1, 2, 3)" );

        SearchSourceBuilder builder = getQueryTranslator().build( query );

        compareStringsIgnoreFormatting( expected_search_result, builder.toString() );
    }

    @Test
    public void testIn_mixed_types()
        throws Exception
    {
        String expected_search_result = "{\n" +
            "  \"from\" : 0,\n" +
            "  \"size\" : " + ContentIndexQuery.DEFAULT_COUNT + ",\n" +
            "  \"query\" : {\n" +
            "    \"filtered\" : {\n" +
            "      \"query\" : {\n" +
            "        \"bool\" : {\n" +
            "          \"should\" : [ {\n" +
            "            \"term\" : {\n" +
            "              \"myfield.number\" : 1.0\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"myfield\" : \"test\"\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"term\" : {\n" +
            "              \"myfield\" : \"2012-03-24\"\n" +
            "            }\n" +
            "          } ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ContentIndexQuery query = createContentQuery( "myField IN (1, 'test', '2012-03-24')" );

        SearchSourceBuilder builder = getQueryTranslator().build( query );

        compareStringsIgnoreFormatting( expected_search_result, builder.toString() );
    }


    @Test(expected = IndexQueryException.class)
    public void testIn_empty()
        throws Exception
    {
        ContentIndexQuery query = createContentQuery( "myField IN ()" );

        getQueryTranslator().build( query );
    }

}
