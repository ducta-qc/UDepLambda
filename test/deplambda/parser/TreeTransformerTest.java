/**
 * 
 */
package deplambda.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deplambda.parser.TreeTransformer;
import deplambda.util.DependencyTree;
import deplambda.util.Sentence;
import deplambda.util.TransformationRuleGroups;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.SimpleLogicalExpressionReader;
import edu.uw.cs.lil.tiny.mr.language.type.MutableTypeRepository;
import edu.uw.cs.utils.composites.Pair;

/**
 * @author Siva Reddy
 *
 */
public class TreeTransformerTest {

  TransformationRuleGroups treeTransformationRules;
  TransformationRuleGroups lambdaAssignmentRules;
  TransformationRuleGroups relationRules;
  JsonObject jsonSentence;
  MutableTypeRepository types = new MutableTypeRepository();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    String sentenceString =
        "{\"entities\": [{\"start\": 0, \"ner\": \"ORGANIZATION\", \"end\": 1, \"entity\": \"m.0k8z\"}, {\"start\": 3, \"ner\": \"PERSON\", \"end\": 4, \"entity\": \"m.06y3r\"}, {\"start\": 8, \"ner\": \"MISC\", \"end\": 9, \"entity\": \"m.04yg_s\"}], \"words\": [{\"word\":\"Apple\",\"head\":2,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Inc.\",\"head\":8,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"which\",\"head\":6,\"tag\":\"WP\",\"label\":\"dobj\"}, {\"word\":\"Steve\",\"head\":5,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"Jobs\",\"head\":6,\"tag\":\"NNP\",\"label\":\"nsubj\"}, {\"word\":\"founded\",\"head\":2,\"tag\":\"VBD\",\"label\":\"rcmod\", \"lemma\":\"found\"}, {\"word\":\",\",\"head\":2,\"tag\":\".\",\"label\":\"p\"}, {\"word\":\"developed\",\"head\":0,\"tag\":\"VBD\",\"label\":\"ROOT\"}, {\"word\":\"iPod\",\"head\":10,\"tag\":\"NNP\",\"label\":\"nn\"}, {\"word\":\"shuffle\",\"head\":8,\"tag\":\"NN\",\"label\":\"dobj\"}, {\"word\":\".\",\"head\":8,\"tag\":\".\",\"label\":\"p\"}]}";
    JsonParser jsonParser = new JsonParser();
    jsonSentence = jsonParser.parse(sentenceString).getAsJsonObject();

    // @formatter:off
    String relations =
        "relation {\n" + 
        "  name: \"l-dobj\"\n" +  
        "  priority: 1\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-p\"\n" +  
        "  priority: 3\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-rcmod\"\n" +  
        "  priority: 2\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-wh-dobj\"\n" +  
        "  priority: 3\n" + 
        "}\n" +
        "relation {\n" + 
        "  name: \"l-nsubj\"\n" +  
        "  priority: 4\n" + 
        "}\n";
    
    String treeTransformationRulesString =
        "rulegroup {\n" + 
        "  name: \"relative clause (all rcmod)\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"dummy rule that should not to be applied\"\n" + 
        "    priority: 6\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/)\"\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"l-rcmod-wrong\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "  rule {\n" + 
        "    name: \"relative obj extraction\"\n" + 
        "    priority: 5\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/) < (/l-(dobj|dep)/=whchild < /t-W.*/)\"\n" + 
        "    # Apple, which Jobs \"founded\", developed Iphone.\n" + 
        "    # (l-nsubj (l-p (l-p (l-dobj w-developed w-Iphone) w-,) w-.) (l-rcmod (l-p w-Apple w-,) (l-wh-dobj (l-BIND (l-nsubj (l-dobj w-founded v-f0) w-Jobs) v-f0) w-which)))\n" + 
        "    transformation {\n" + 
        "      target: \"whchild\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"l-wh-dobj\"\n" + 
        "    }\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: ADD_CHILD\n" + 
        "      label: \"l-dobj\"\n" + 
        "      child: \"v-f\"\n" + 
        "    }\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: ADD_CHILD\n" + 
        "      label: \"l-BIND\"\n" + 
        "      child: \"v-f\"\n" + 
        "    }\n" + 
        "  }\n" +
        "  rule {\n" + 
        "    name: \"dummy rule that have to be applied\"\n" + 
        "    priority: 5\n" + 
        "    tregex: \"(l-rcmod=target < /t-V.*/=verb)\"\n" + 
        "    transformation {\n" + 
        "      target: \"verb\"\n" + 
        "      action: CHANGE_LABEL\n" + 
        "      label: \"t-VB\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "}\n" + 
        "";
    
    String lambdaAssignmentRulesString = 
        "rulegroup {\n" + 
        "  name: \"dependency labels\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"main\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"/l-(nsubj|subjpass)/=dep\"\n" + 
        "    transformation {\n" + 
        "      target: \"dep\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"(lambda $F:vp (lambda $G:np ($F $G)))\"\n" + 
        "    }\n" + 
        "  }\n" +
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"main\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"/l-dobj/=dep\"\n" + 
        "    transformation {\n" + 
        "      target: \"dep\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"(lambda $F:vt (lambda $G:np ($F $G)))\"\n" + 
        "    }\n" + 
        "  }\n" +
        "  rule {\n" + 
        "    name: \"rcmod\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"l-rcmod=dep\"\n" + 
        "    # IBM bought \"DataMirror\", which had \"purchased\" ConstellarHub.\n" + 
        "    # \"Apple\", which Jobs founded, \"developed\" iPhone.\n" + 
        "    transformation {\n" + 
        "      target: \"dep\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"(lambda $F:np (lambda $G:vp (lambda $Z:z (exists:ex $E:z (and:cj ($F $Z) ($G $F $E))))))\"\n" + 
        "    }\n" + 
        "  }\n" +
        "  rule {\n" + 
        "    name: \"rcmod\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"l-BIND=dep\"\n" + 
        "    transformation {\n" + 
        "      target: \"dep\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"empty:bind\"\n" + 
        "    }\n" + 
        "  }\n" +
        "}" + 
        "rulegroup {\n" + 
        "  name: \"verbs\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"transitive verb\"\n" + 
        "    priority: 3\n" + 
        "    tregex: \"/w-.*/=verb [$ /t-V.*/ $ l-dobj !$ l-nsubjpass]\"\n" + 
        "    # Cameron directed Titanic.\n" + 
        "    transformation {\n" + 
        "      target: \"verb\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"(lambda $F2:np (lambda $F1:np (lambda $E:z "
        + "(exists:ex $X:z (exists:ex $Y:z (and:cj (p_EVENT.ENTITY_{verb}.arg_1:epd $E $X) "
        + "(p_EVENT.ENTITY_{verb}.arg_2:epd $E $Y) ($F1 $X) ($F2 $Y)))))))\"\n" + 
        "    }\n" + 
        "  }" +
        "}" +
        "rulegroup {\n" + 
        "  name: \"nouns\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"any noun or pronoun\"\n" + 
        "    priority: 3\n" + 
        "    tregex: \"/w-.*/=noun $ /t-(N.*|PRP.*|CD|[$])/\"\n" + 
        "    # \"I\" gave him a \"pencil\".\n" + 
        "    transformation {\n" + 
        "      target: \"noun\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"(lambda $X:z (p_TYPE_{noun}:tpd $X))\"\n" + 
        "    }\n" + 
        "  }" + 
        "}" + 
        "rulegroup {\n" + 
        "  name: \"virtual\"\n" + 
        "  priority: 1\n" + 
        "  rule {\n" + 
        "    name: \"virtual default\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"/v-.*/=target\"\n" + 
        "    transformation {\n" + 
        "      target: \"target\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"{target}:np\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "}\n" + 
        "rulegroup {\n" + 
        "  name: \"defaults\"\n" + 
        "  priority: 10\n" + 
        "  rule {\n" + 
        "    name: \"word default\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"/w-.*/=word\"\n" + 
        "    transformation {\n" + 
        "      target: \"word\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"empty:null\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "  rule {\n" + 
        "    name: \"dep default\"\n" + 
        "    priority: 1\n" + 
        "    tregex: \"/l-.*/=dep\"\n" + 
        "    transformation {\n" + 
        "      target: \"dep\"\n" + 
        "      action: ASSIGN_LAMBDA\n" + 
        "      lambda: \"empty:ba\"\n" + 
        "    }\n" + 
        "  }\n" + 
        "}\n" + 
        "" ;
    // @formatter:on

    File treeTransformationRuleFile =
        File.createTempFile("tree-transfomration-rules", ".tmp");
    BufferedWriter bw =
        new BufferedWriter(new FileWriter(treeTransformationRuleFile));
    bw.write(treeTransformationRulesString);
    bw.close();
    treeTransformationRules =
        new TransformationRuleGroups(
            treeTransformationRuleFile.getAbsolutePath());
    treeTransformationRuleFile.delete();

    File lambdaAssignmentRuleFile =
        File.createTempFile("lambda-assignment-rules", ".tmp");
    bw = new BufferedWriter(new FileWriter(lambdaAssignmentRuleFile));
    bw.write(lambdaAssignmentRulesString);
    bw.close();
    lambdaAssignmentRules =
        new TransformationRuleGroups(lambdaAssignmentRuleFile.getAbsolutePath());
    lambdaAssignmentRuleFile.delete();

    File relationRuleFile = File.createTempFile("relation-rules", ".tmp");
    bw = new BufferedWriter(new FileWriter(relationRuleFile));
    bw.write(relations);
    bw.close();
    relationRules =
        new TransformationRuleGroups(relationRuleFile.getAbsolutePath());
    relationRuleFile.delete();

    types.getTypeCreateIfNeeded("(z e)");
    types.getTypeCreateIfNeeded("{s <z,t>}");
    types.getTypeCreateIfNeeded("{np <z,t>}");
    types.getTypeCreateIfNeeded("{vp <np,s>}");
    types.getTypeCreateIfNeeded("{vt <np,vp>}");
    types.getTypeCreateIfNeeded("{ppv <vp,vp>}");
    types.getTypeCreateIfNeeded("{ppn <np,np>}");
    types.getTypeCreateIfNeeded("{ex <z,<t,t>>}");
    types.getTypeCreateIfNeeded("{epd <z*,t>}");
    types.getTypeCreateIfNeeded("{tpd <z,t>}");
    types.getTypeCreateIfNeeded("{cj <t*,t>}");

    LogicLanguageServices.setInstance(new LogicLanguageServices.Builder(types,
        new FlexibleTypeComparator()).closeOntology(false)
        .setNumeralTypeName("i").build());
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#applyRuleGroupsOnTree(TransformationRuleGroups, deplambda.util.DependencyTree)}
   * .
   */
  @Test
  public final void testApplyRuleGroupsOnTree() {
    Sentence sentence = new Sentence(jsonSentence);

    // TreeTransformationRules for modifying the structure of a tree.
    assertTrue(TreeTransformer.applyRuleGroupsOnTree(treeTransformationRules,
        sentence.getRootNode()));
    assertEquals(
        "(l-ROOT w-developed t-VBD (l-nsubj w-Inc. t-NNP (l-rcmod w-found t-VB (l-wh-dobj w-which t-WP) (l-nsubj w-Jobs t-NNP) (l-dobj v-f) (l-BIND v-f)) (l-p w-, t-.)) "
            + "(l-dobj w-shuffle t-NN) (l-p w-. t-.))", sentence.getRootNode()
            .toString());

    // Binarization of tree based on relation priority.
    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals(
        "(l-nsubj (l-p (l-dobj w-developed w-shuffle) w-.) (l-p (l-rcmod w-Inc. (l-BIND (l-nsubj (l-wh-dobj (l-dobj w-found v-f) w-which) w-Jobs) v-f)) w-,))",
        binarizedTreeString);

    TreeTransformer.applyRuleGroupsOnTree(lambdaAssignmentRules,
        sentence.getRootNode());

    // Assigning lambda
    DependencyTree foundNode =
        ((DependencyTree) sentence.getRootNode().getChild(2).getChild(2)
            .getChild(0));
    assertEquals(1, foundNode.getNodeLambda().size());
    assertEquals(
        "(lambda $0:<z,t> (lambda $1:<z,t> (lambda $2:z (exists:ex $3:z (exists:ex $4:z (and:cj (p_EVENT.ENTITY_w-found.arg_1:epd $2 $3) (p_EVENT.ENTITY_w-found.arg_2:epd $2 $4) ($1 $3) ($0 $4)))))))",
        foundNode.getNodeLambda().get(0).toString());

    // Composing lambda.
    Pair<String, List<LogicalExpression>> sentenceSemantics =
        TreeTransformer.composeSemantics(sentence.getRootNode(),
            relationRules.getRelationPriority(), false);
    assertEquals(
        "(lambda $0:z (exists:ex $1:z (exists:ex $2:z (and:cj (p_EVENT.ENTITY_w-developed.arg_1:epd $0 $1) (p_EVENT.ENTITY_w-developed.arg_2:epd $0 $2) (exists:ex $3:z (and:cj (p_TYPE_w-Inc.:tpd $1) (exists:ex $4:z (exists:ex $5:z (and:cj (p_EVENT.ENTITY_w-found.arg_1:epd $3 $4) (p_EVENT.ENTITY_w-found.arg_2:epd $3 $5) (p_TYPE_w-Jobs:tpd $4) (p_TYPE_w-Inc.:tpd $5)))))) (p_TYPE_w-shuffle:tpd $2)))))",
        sentenceSemantics.second().get(0).toString());
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#heuristicJoin(LogicalExpression, LogicalExpression)}
   * .
   */
  @Test
  public final void testheuristicJoin() {
    LogicalExpression exp1 =
        SimpleLogicalExpressionReader
            .read("(lambda $f1:vp (exists:ex $e:z ($f1 cameron:np $e)))");
    LogicalExpression exp2 =
        SimpleLogicalExpressionReader
            .read("(lambda $f1:vp (lambda $e:z ($f1 something:np $e)))");
    assertEquals(
        "(lambda $0:<np,s> (and:<t*,t> (exists:ex $1:z ($0 cameron:np $1)) (empty:<<<np,s>,<z,t>>,t> (lambda $2:<np,s> (lambda $3:z ($2 something:np $3))))))",
        TreeTransformer.heuristicJoin(exp1, exp2).toString());
  }

  /**
   * Test method for
   * {@link deplambda.parser.TreeTransformer#binarizeTreeString(deplambda.util.DependencyTree, java.util.Map)}
   * .
   */
  @Test
  public final void testBinarizeTree() {
    Sentence sentence = new Sentence(jsonSentence);
    String binarizedTreeString =
        TreeTransformer.binarizeTree(sentence.getRootNode(),
            relationRules.getRelationPriority());
    assertEquals(
        "(l-nsubj (l-p (l-dobj w-developed w-shuffle) w-.) (l-p (l-rcmod w-Inc. (l-nsubj (l-dobj w-found w-which) w-Jobs)) w-,))",
        binarizedTreeString);
  }
}