//#full-example
package com.example


import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.example.GreeterMain.SayHello

//#greeter-actor
object Greeter {
  //#message "Greet" sent by Greeter
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  //#message "Greeted" responded to Greeter
  final case class Greeted(whom: String, from: ActorRef[Greet])

  //#typed Behaviour[Greet] - meaning it receives Greet / Greet subtypes
  def apply(): Behavior[Greet] = Behaviors.receive { (ctx, message) =>
    // Hello
    ctx.log.info("Hello {}!", message.whom)
    //#greeter-send-messages
    message.replyTo ! Greeted(message.whom, ctx.self)
    //#greeter-send-messages
    Behaviors.same
  }
}
//#greeter-actor

//#greeter-bot
object GreeterBot {

  def apply(max: Int): Behavior[Greeter.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[Greeter.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! Greeter.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}
//#greeter-bot

//#greeter-main
object GreeterMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      //#create-actors
      val greeter = context.spawn(Greeter(), "greeter")
      //#create-actors

      Behaviors.receiveMessage { message =>
        //#create-actors
        val replyTo = context.spawn(GreeterBot(max = 3), message.name)
        //#create-actors
        greeter ! Greeter.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
}
//#greeter-main

//#main-class
object PekkoQuickstart extends App {
  //#actor-system
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "PekkoQuickstart")
  //#actor-system

  //#main-send-messages
  greeterMain ! SayHello("Charles")
  //#main-send-messages
}
//#main-class
//#full-example
