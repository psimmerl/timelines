#!/home/kenjo/.groovy/coatjava/bin/run-groovy
import org.jlab.io.hipo.HipoDataSource
import org.jlab.groot.data.TDirectory
import mon.GoodElectron
import mon.ParticleMon
import mon.Elastic
//import com.google.gson.Gson

//def jdata = []

for(fname in args) {
  int run = fname.split("/")[-1].split('\\.')[0][-4..-1].toInteger()
  int fnum = fname.split(".evio.")[-1].split('-')[0].split("\\.")[0].toInteger()

  def monengines = [electron: new GoodElectron(), part: new ParticleMon(), elastic: new Elastic()]

  def reader = new HipoDataSource()
  reader.open(fname)

  while(reader.hasEvent()) {
    def event = reader.getNextEvent()
    monengines.values().each{it.processEvent(event)}
  }

  //monengines.each{id, eng->
  //  jdata.add([run:run, fnum:fnum, monid:id, data:eng.data])
  //}

  reader.close()

  def out = new TDirectory()
  monengines.each{id, eng->
    eng.plots.each{name, dss ->
      out.mkdir("/$id/$name")
      out.cd("/$id/$name")
      dss.values().each{out.addDataSet(it)}
    }
  }
  out.writeFile(String.format("monplots_%d_%05d.hipo", run, fnum))
}

//int run = args[0].split("/")[-1].split('\\.')[0][-4..-1].toInteger()
//int fnum = args[0].split(".evio.")[-1].split('-')[0].split("\\.")[0].toInteger()

//def gson = new Gson()
//new File(String.format("mondata_%d_%05d.json", run,fnum)).write(gson.toJson(jdata))
