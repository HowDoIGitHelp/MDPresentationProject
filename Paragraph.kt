import java.io.File

class Document{
  val paragraphs: MutableList<Paragraph> = ArrayList()
  fun latex(): String{
    var latexString =
      """
        \documentclass{beamer}
        \title{Sample title}
        \author{Anonymous}

        \begin{document}

      """.trimIndent()

    for(paragraph in this.paragraphs)
      latexString += paragraph.framedDisplay()

    latexString +=
    """
      \end{document}

    """.trimIndent()
    return latexString
  }
}

interface Paragraph{
  fun rawString(): String
  fun displayString(): String
  fun framedDisplay(): String
}

class Header(val contents: String) : Paragraph {
  val level: Int
  init {
    level = Regex("#+").find(contents)?.value?.length ?: throw Exception("Missing octothorpes in header")
  }
  override fun rawString(): String{
    return contents
  }
  override fun displayString(): String{
    return this.rawString().substring(Regex("#+(\\s)*").find(this.rawString())?.value?.length ?: 0)
  }
  override fun framedDisplay(): String{
    var framedString: String = "\\section{" + this.displayString() + "}\n"
    return framedString
  }
}

class Text(val contents: String) : Paragraph {
  val sentences: MutableList<Sentence> = ArrayList()
  init {
    val rawSentences = contents.replace(Regex("\\.(\\s)*$"),"").split(Regex("\\.(\\s)*")) //remove the last period before splitting into separate sentences
    for(rawSentence in rawSentences){
      if(rawSentence.contains(Regex("\\*\\*.*\\*\\*")) || rawSentence.contains(Regex("\\*.*\\*")))
        sentences.add(ImportantSentence(rawSentence))
      else
        sentences.add(Sentence(rawSentence))
    }
  }
  override fun rawString(): String{
    return contents
  }
  override fun displayString(): String{
    var displayString = ""
    for(sentence in this.sentences){
      displayString += sentence.displayString() + ".\n"
    }
    return displayString
  }
  override fun framedDisplay(): String{
    var framedString: String = ""
    for(sentence in this.sentences){
      framedString += sentence.framedDisplay()
    }
    return framedString
  }
}

class NumberedItem(val contents: String): Paragraph{
  val index: Int
  val item: String
  val level: Int
  init{
    val indexPlusItem: List<String> = contents.split(Regex("(\\.(\\s)*)"),2)
    this.index = trimPreSpaces(indexPlusItem[0]).toInt()
    this.item = indexPlusItem[1]
    this.level = contents.split(Regex("(\\d+\\.( )*)"))[0].length / 3 //3 is the tab length

    println(this.index.toString() + " " + this.item + " " + this.level.toString())
  }
  override fun rawString(): String{
    return contents
  }
  override fun displayString(): String{
    return "none\n"
  }
  override fun framedDisplay(): String{
    return "none\n"
  }
}

open class Sentence(val contents: String){
  open fun rawString(): String{
    return contents
  }
  open fun displayString(): String{
    return this.rawString()
  }
  open fun framedDisplay(): String{
    return ""
  }
}

class ImportantSentence(contents: String): Sentence(contents){
  override fun framedDisplay(): String{
    var framedString: String =
      """
        \begin{frame}
        \frametitle{\insertsection}

      """.trimIndent()
      framedString += this.displayString()+"\n"
      framedString +=
      """
        \end{frame}

      """.trimIndent()
    return framedString
  }
  override fun displayString(): String{
    val strong: String = contents.replace(Regex("\\*\\*.*\\*\\*"),{matchresult -> strongString(matchresult.value)})
    val emphasized:String = strong.replace(Regex("\\*.*\\*"),{matchresult -> emphasizedString(matchresult.value)})
    return emphasized
  }
}

fun emphasizedString(str: String): String{
  return "\\emph{" + str.substring(1..str.length-2) + "}"
}

fun strongString(str: String): String{
  return "\\textbf{" + str.substring(2..str.length-3) + "}"
}

fun trimPreSpaces(str: String): String{
  return str.replace(Regex("^( )*"),"")
}

fun parse(file: File){
  val lines: List<String> = file.useLines { it.toList() }
  val document: Document = Document()
  for(line in lines){
    if (Regex("#+.*") matches line)
      document.paragraphs.add(Header(line))
    else if (line.matches(Regex("(( )*\\d+\\.( )*).*")))
      document.paragraphs.add(NumberedItem(line))
    else if (line != "")
      document.paragraphs.add(Text(line))


  }

  //for(paragraph in document.paragraphs){
  //  println(paragraph.displayString())
  //}

  println(document.latex())
  File("output.tex").writeText(document.latex())
}

fun main(){
  parse(File("test.md"))
}
