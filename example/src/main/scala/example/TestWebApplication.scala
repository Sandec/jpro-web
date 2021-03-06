package example

import com.jpro.web._
import simplefx.core._
import simplefx.all._
import com.jpro.web.Util._
import com.jpro.web.sessionmanager.SessionManager
import com.jpro.webapi.{HTMLView, WebAPI}
import de.sandec.jmemorybuddy.JMemoryBuddyLive
import org.controlsfx.control.PopOver

import scala.collection.JavaConverters.asScalaBufferConverter

class MyApp(stage: Stage) extends WebApp(stage) {

  stylesheets ::= "test.css"



  addRoute { case ""                => new MainView()}
  addRoute { case "/"                => new MainView()}
  addRoute { case "/?page=main"      => new MainView()}
  addRoute { case "/?page=green"      => new GreenView()}
  addRoute { case "/?page=orange"      => new OrangeView()}
  addRoute { case "/?page=sub"       => new SubView()}
  addRoute { case "/?page=redirect"  => Redirect("/?page=sub")}
  addRoute { case "/?page=paralax"   => new ParalaxPage()}
  addRoute { case "/?page=pdf"       => new PDFTest()}
  addRoute { case "/?page=leak"       => new LeakingPage()}
  addRoute { case "/?page=collect"       => new CollectingPage()}
  addRoute { case "/?page=jmemorybuddy"       => new JMemoryBuddyPage()}
  addRoute { case "/?page=it's\" tricky" => new MainView()}
  addRoute { case x                  => new UnknownPage(x)}

 // addTransition{ case (null,view2,true ) => PageTransition.InstantTransition }
 // addTransition{ case (view,view2,true ) => PageTransition.MoveDown }
 // addTransition{ case (view,view2,false) => PageTransition.MoveUp }
}

class Header(var sessionManager: SessionManager) extends HBox {
  padding = Insets(10)
  spacing = 10
  class HeaderLink(str: String, url: String) extends Label (str) {
    styleClass ::= "header-link"
    if(!url.isEmpty) {
      setLink(this, url, Some(str))
    }
  }
  this <++ new HeaderLink("main"    , "/?page=main")
  this <++ new HeaderLink("subpage" , "/?page=sub" )
  this <++ new HeaderLink("redirect", "/?page=redirect" )
  this <++ new HeaderLink("tricky!" , "/?page=it's\" tricky" )
  this <++ new HeaderLink("google"  , "http://google.com" )
  this <++ new HeaderLink("paralax" , "/?page=paralax" )
  this <++ new HeaderLink("dead"    , "/?page=as df" )
  this <++ new HeaderLink("green"   , "/?page=green" )
  this <++ new HeaderLink("orange"  , "/?page=orange" )
  this <++ new HeaderLink("pdf"     , "/?page=pdf" )
  this <++ new HeaderLink("leak"    , "/?page=leak" )
  this <++ new HeaderLink("collect"    , "/?page=collect" )
  this <++ new HeaderLink("jmemorybuddy"    , "/?page=jmemorybuddy" )
  this <++ new HeaderLink("No Link" , "" )


  this <++ new Button("Backward") {
    disable <-- (!WebAPI.isBrowser && sessionManager.historyBackward.isEmpty)
    onAction --> {
      goBack(this)
    }
  }
  this <++ new Button("Forward") {
    disable <-- (!WebAPI.isBrowser && sessionManager.historyForward.isEmpty)
    onAction --> {
      goForward(this)
    }
  }
}

class Footer(sessionManager: SessionManager) extends HBox {
  spacing = 10
  this <++ new Label("asd")
  this <++ new Label("url: " + sessionManager.url)
  this <++ new Button("refresh") {
    onAction --> {
      Util.refresh(this)
    }
  }
}

trait Page extends View {
  override lazy val realContent = {
    new VBox {
  // Cousing leak? style = "-fx-background-color: white;"
    //  transform = Scale(1.3,1.3)
      spacing = 10
      this <++ new Header(sessionManager)
      val theContent = content
      javafx.scene.layout.VBox.setVgrow(theContent,Priority.ALWAYS)
      this <++ theContent
      this <++ new Footer(sessionManager)
      //this <++ new Header(sessionManager)
    }
  }
}

class UnknownPage(x: String) extends Page {
  def title = "Unknown page: " + x
  def description = "Unknown page: " + x

  override def fullscreen = false

  def content = new Label("UNKNOWN PAGE: " + x) { font = new Font(60)}
}
class OrangeView() extends Page {
  def title = "Orange Page"
  def description = "desc Main"

  override def fullscreen = false

  def content = new StackPane { style = "-fx-background-color: orange;"}
}
class GreenView() extends Page {
  def title = "Green Page"
  def description = "desc Main"

  override def fullscreen = false

  def content = new StackPane { style = "-fx-background-color: green;"}
}

class MainView extends Page {
  def title = "Main"
  def description = "desc Main"

  lazy val content = new VBox {
    spacing = 100
    def addGoogle: Node = {
      new StackPane(new Label("GOOGL") {
        font = new Font(60);
      }) {
        this <++ new HTMLView {
          setContent(
            """
              |<a style="display: block; width: 100%; height: 100%; background-color: #66666666;" href="http://google.com"></a>
            """.stripMargin
          )
        }
      }
    }
    this <++ addGoogle
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ new Button("Open Popup") { button =>
      onAction --> {
        val content = new VBox { box =>
          this <++ new Label("Im A Link to Google!") {
            setLink(this,"http://google.com")
          }
          this <++ addGoogle
        }
        new PopOver(content) {
        }.show(button)
      }
    }
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ new Label(" label 123") { font = new Font(60)}
    this <++ addGoogle
    this <++ new Label("paralax" ) { font = new Font(60); setLink(this, "/?page=paralax" ) }

  }
}

class SubView extends Page {
  def title = "SubView"
  def description = "desc Sub"
  override def fullscreen=true

  lazy val content = new VBox {
    this <++ new Label("SUBVIEW") { font = new Font(60)}
    this <++ new Label("I'm fullscreen!") { font = new Font(60)}
  }
}

class PDFTest extends Page {
  def title = "pdf"
  def description = "pdf desc"

  lazy val content = new VBox {
    this <++ new Label("PAGE 1") { font = new Font(60)}
    this <++ new HTMLView("<div style=\"break-after:page\"></div>")
    this <++ new Label("PAGE 2") { font = new Font(60)}
    this <++ new HTMLView("<div style=\"break-after:page\"></div>")
    this <++ new Label("PAGE 3") { font = new Font(60)}
  }
}
object LeakingPage {
  var instances: List[Page] = Nil
}
class LeakingPage extends Page {
  def title = "leak"
  def description = "leaks"

  LeakingPage.instances ::= this

  val content = new VBox {
    this <++ new Label("Leaks") { font = new Font(60)}
  }
}
class CollectingPage extends Page {
  def title = "collect"
  def description = "collect"

  LeakingPage.instances ::= this

  override def onClose(): Unit = {
    println("onClose called!")
    LeakingPage.instances = LeakingPage.instances.filter(_ != this)
  }

  val content = new VBox {
    this <++ new Label("Leaks") { font = new Font(60)}
  }
}
class JMemoryBuddyPage extends Page {
  def title = "buddy"
  def description = "buddy"

  System.gc()

  val content = new VBox {
    this <++ new Label() {
      text = JMemoryBuddyLive.getReport.toString
      wrapText = true
    }
    this <++ new Label() {
      text = JMemoryBuddyLive.getReport.uncollectedEntries.asScala.map(_.name).mkString("\n")
      wrapText = true
    }
  }
}

class ParalaxPage extends Page {
  def title = "Paralax"
  def description = "desc Para"

  //override def saveScrollPosition: Boolean = false

  val img1 = getClass().getResource("/images/img1.jpg")

  val content = new StackPane {
    this <++ new Region {
      maxWidthProp = 5
      style = "-fx-background-color: green;"
    }
    this <++ new VBox {
      spacing = 200

      this <++ new ParallaxView(img1) {
        minWH = (250,300)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new ParallaxView(img1) {
        minWH = (250,400)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new ParallaxView(img1) {
        minWH = (250,400)
        style = "-fx-border-width:1; -fx-border-color:black;"
      }
      this <++ new Label("asdf")
      this <++ new Label("asdf")
      this <++ new Label("asdf")
    }
    this <++ new Region {
      maxWidthProp = 5
      style = "-fx-background-color: green;"
    }
  }

}



object TestWebApplication extends App
@SimpleFXApp class TestWebApplication {
  val app = new MyApp(stage)
  if(WebAPI.isBrowser) {
    root = app
  } else {
    scene = new Scene(new ScrollPane(app) {
      fitToWidth = true
    }, 1400,800)
  }
  app.start(SessionManager.getDefault(app,stage))
}
