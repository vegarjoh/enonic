/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.query;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

import com.enonic.cms.core.content.index.ContentIndexQuery;

public class QueryTranslator_not_equalsTest
    extends QueryTranslatorTestBase
{

    @Test
    public void testNotQuery_key()
        throws Exception
    {
        String expected_search_result = "{\n" +
            "  \"from\" : 0,\n" +
            "  \"size\" : " + ContentIndexQuery.DEFAULT_COUNT + ",\n" +
            "  \"query\" : {\n" +
            "    \"filtered\" : {\n" +
            "      \"query\" : {\n" +
            "        \"bool\" : {\n" +
            "          \"must\" : {\n" +
            "            \"match_all\" : { }\n" +
            "          },\n" +
            "          \"must_not\" : {\n" +
            "            \"ids\" : {\n" +
            "              \"type\" : \"content\",\n" +
            "              \"values\" : [ \"100\" ]\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ContentIndexQuery query = createContentQuery( "key != 100" );

        SearchSourceBuilder builder = getQueryTranslator().build( query );

        compareStringsIgnoreFormatting( expected_search_result, builder.toString() );

    }

    @Test
    public void testNotQuery_range_and_key()
        throws Exception
    {
        String expected_search_result = "{\n" +
            "  \"from\" : 0,\n" +
            "  \"size\" : " + ContentIndexQuery.DEFAULT_COUNT + ",\n" +
            "  \"query\" : {\n" +
            "    \"filtered\" : {\n" +
            "      \"query\" : {\n" +
            "        \"bool\" : {\n" +
            "          \"must\" : [ {\n" +
            "            \"term\" : {\n" +
            "              \"title\" : \"test\"\n" +
            "            }\n" +
            "          }, {\n" +
            "            \"bool\" : {\n" +
            "              \"must\" : {\n" +
            "                \"match_all\" : { }\n" +
            "              },\n" +
            "              \"must_not\" : {\n" +
            "                \"ids\" : {\n" +
            "                  \"type\" : \"content\",\n" +
            "                  \"values\" : [ \"100\" ]\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          } ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ContentIndexQuery query = createContentQuery( "title = 'test' AND key != 100" );

        SearchSourceBuilder builder = getQueryTranslator().build( query );

        compareStringsIgnoreFormatting( expected_search_result, builder.toString() );

    }
}
