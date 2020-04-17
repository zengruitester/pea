package pea.app.model

trait BatchJob {
  val worker: PeaMember
  val load: LoadMessage
}
