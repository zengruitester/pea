package pea.app.model.job

import pea.app.model.multiscene.MultisScenariosMessage
import pea.app.model.{PeaMember, SingleJob}

case class RunHttpScenariosSingleJobs(
                                        worker: PeaMember,
                                        load: MultisScenariosMessage
                                      ) extends SingleJob
