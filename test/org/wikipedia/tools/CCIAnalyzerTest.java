/**
 *  @(#)CCIAnalyzerTest.java 0.03 04/01/2020
 *  Copyright (C) 2019 - 20xx MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.wikipedia.tools;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.wikipedia.*;

/**
 *  Unit tests for {@link CCIAnalyzer}.
 *  @author MER-C
 */
public class CCIAnalyzerTest
{
    private final Wiki enWiki;
    private final CCIAnalyzer analyzer;
    
    public CCIAnalyzerTest()
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        analyzer = new CCIAnalyzer();
    }
    
    @Test
    public void removeReferences()
    {
        assertEquals("Test: plain ref.", CCIAnalyzer.removeReferences("Test: plain ref<ref>Test reference</ref>."));
        assertEquals("Test: named ref.", CCIAnalyzer.removeReferences("Test: named ref<ref name=\"Test\">Test reference</ref>."));
        assertEquals("Test: reused ref.", CCIAnalyzer.removeReferences("Test: reused ref<ref name=\"Test\" />."));
        assertEquals("Test: unbalanced ref 1<ref>.", CCIAnalyzer.removeReferences("Test: unbalanced ref 1<ref>."));
        assertEquals("Test: unbalanced ref 2<ref name=\"unbalanced\">.", CCIAnalyzer.removeReferences("Test: unbalanced ref 2<ref name=\"unbalanced\">."));
        assertEquals("Test: combined. Sentence 2.", CCIAnalyzer.removeReferences(
            "Test: combined<ref name=\"Test\">Test reference</ref>. Sentence 2<ref name=\"Test\" />."));
        assertEquals("Test: combined before. Sentence 2.", CCIAnalyzer.removeReferences(
            "Test: combined before<ref name=\"Before\" />. Sentence 2<ref>Test reference</ref>."));
        assertEquals("Test: multiple.", CCIAnalyzer.removeReferences("Test: multiple<ref>Reference 1</ref><ref>Reference 2</ref>."));
        
        // INTEGRATION TEST
        // the second diff contains references only
        String cci = "*[[:Smiley (1956 film)]] (2 edits): [[Special:Diff/476809081|(+460)]][[Special:Diff/446793589|(+205)]]";
        analyzer.setFilteringFunction(CCIAnalyzer::removeReferences);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        analyzer.loadString(enWiki, cci);
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/446793589|(+205)]]"), analyzer.getMinorEdits());
    }
    
    @Test
    public void removeExternalLinks()
    {
        assertEquals("Test  Test2", CCIAnalyzer.removeExternalLinks("Test [http://example.com Test link] Test2"));
        assertEquals("*", CCIAnalyzer.removeExternalLinks("*[http://example.com Test link]"));
    }
    
    @Test
    public void whitelistCull()
    {
        // INTEGRATION TEST
        // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]] - AFD
        String cci = "*[[:List of science fiction comedy works]] (1 edit): [[Special:Diff/924018716|(+458)]]";
        analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(CCIAnalyzer::whitelistCull);
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/924018716|(+458)]]"), analyzer.getMinorEdits());
    }
    
    @Test
    public void wordCountCull()
    {
        // INTEGRATION TEST
        String cci =
            // 13 words - checks word count threshold
            "*'''N''' [[:Urmitz]] (1 edit): [[Special:Diff/154400451|(+283)]]" + 
            // 15 words, but two of them are just wikitext remnants - should be
            // removed as punctuation
            "*'''N''' [[:SP-354]] (1 edit): [[Special:Diff/255072765|(+286)]]";
        analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 12));
        analyzer.analyzeDiffs();
        assertTrue(analyzer.getMinorEdits().isEmpty());
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 13));
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/154400451|(+283)]]", "[[Special:Diff/255072765|(+286)]]"), analyzer.getMinorEdits());
    }
    
    @Test
    public void listItemCull()
    {
        assertFalse(CCIAnalyzer.listItemCull("*[http://example.com External link]"));
        assertFalse(CCIAnalyzer.listItemCull("*[[Wikilink]]"));
    }
    
    @Test
    public void fileAdditionCull()
    {
        String filestring = ("[[File:St Lawrence Jewry, City of London, UK - Diliff.jpg"
            + "|thumb|right|400px|The interior of St Lawrence Jewry, the official church of the Lord Mayor "
            + "of London, located next to Guildhall in the City of London.]]").toLowerCase();
        assertFalse(CCIAnalyzer.fileAdditionCull(filestring));
    }
    
    /**
     *  Miscellaneous integration tests.
     */
    @Test
    public void loadString()
    {
        // WikitextUtils.removeComments
        // from [[Wikipedia:Contributor copyright investigations/Kailash29792 02]]
        String cci = "*[[:List of science fiction comedy works]] (1 edit): [[Special:Diff/924018716|(+458)]]";
        analyzer.loadString(enWiki, cci);
        analyzer.setCullingFunction(diff -> analyzer.wordCountCull(diff, 9));
        analyzer.analyzeDiffs();
        assertTrue(analyzer.getMinorEdits().isEmpty());
        analyzer.setFilteringFunction(WikitextUtils::removeComments);
        analyzer.analyzeDiffs();
        assertEquals(List.of("[[Special:Diff/924018716|(+458)]]"), analyzer.getMinorEdits());
        
        // from [[Wikipedia:Contributor copyright investigations/Dr. Blofeld 40]] - infoboxes
        // cci = "*[[:Ann Thongprasom]] (1 edit): [[Special:Diff/130352114|(+460)]]" +
        // "*[[:Shoma Anand]] (1 edit): [[Special:Diff/130322991|(+460)]]";
    }
}