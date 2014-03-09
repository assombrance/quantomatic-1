package quanto.core
import quanto.util.json._
import JsonValues._

class Core(var controller: String, executable: String) {
  var rid = 0
  val process = new CoreProcess
  
  def start() { process.startCore(executable) }
  def stop() { process.killCore(waitForExit = true) }
  def kill() { process.killCore(waitForExit = false) }

  def request(module: String, function: String, input: Json, ctrl: String = controller): Json =
  {
    JsonObject(
      "request_id" -> rid,
      "controller" -> ctrl,
      "module"     -> module,
      "function"   -> function,
      "input"      -> input
    ).writeTo(process.stdin)

    process.stdin.flush()

    Json.parse(process.stdout) match {
      case JsonObject(map) =>
        try {
          val output = map("output")
          if (map("success")) output
          else
            if ((output / "code").intValue == -1) throw new CoreProtocolException((output / "message").stringValue)
            else throw new CoreUserException((output / "message").stringValue, (output / "code").intValue)
        } catch {
          case e: NoSuchElementException =>
            throw new CoreProtocolException(e.toString + " for JSON: " + JsonObject(map).toString)
        }
      case _ => throw new CoreProtocolException("Expected JSON object as core response")
    }
  }
  
  // functions built in to the controller
  def help(module: String, function: String) : String = 
    this.request("system", "help", JsonObject("module"->module,"function"->function), "!!")
    
  def help(module: String) : String = 
    this.request("sytem", "help", JsonObject("module"->module), "!!")
  
  def version(): String = this.request("system", "version", JsonNull, "!!")
}


