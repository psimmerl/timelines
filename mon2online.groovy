#!/group/clas12/packages/coatjava/6.3.1/bin/run-groovy

import com.google.gson.Gson
import java.io.FileReader
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.jnp.hipo.io.HipoReader;

def gson = new Gson()

def rawdata = args.findAll{it.endsWith('.json')}.collect{fname->gson.fromJson(new FileReader(fname), List.class)}.flatten()
def hfiles = args.findAll{it.endsWith('.hipo')}

def mondata = [:].withDefault{[:].withDefault{[]}}
hfiles.each{
  int run = it.split("_")[1].toInteger()
  int myfnum = it.split("_")[2].split("\\.")[0].toInteger()
  def inp = new TDirectory()
  inp.readFile(it)
  inp.getCompositeObjectList(inp).each{
    def h1 = inp.getObject(it)
    def entry = [(it.split("/")[3]) : h1.integral()]
    def id = String.format('/%s/%s',it.split("/")[1],it.split("/")[2]);
    def containsMem = false;
    mondata[id][run].each{s2->
      if(myfnum == s2.get('fnum')) {
	s2['data']<<entry;
	containsMem = true; 
      }
    }
    if(!containsMem){
        mondata[id][run].add([fnum:myfnum, data:entry])
    }
  }
}

rawdata.each{
  int run = it.run
  int fnum = it.fnum
  it.data.each{name,entry->
    def id = String.format('/%s/%s',it.monid,name)
    mondata[id][run].add([fnum:fnum, data:entry])
  }
}

def fc = mondata['/fc/fcup']
fc = fc.collectEntries{run, flist ->
  def fcm = flist.collectEntries{ [((int)it.fnum): it.data.max-it.data.min] }
  fcm.tot = flist.sum{it.data.max-it.data.min}
  return [(run): fcm]
}

mondata.findAll{!it.key.contains('/fc/fc')}.each{id, runmap ->
  def gr = [:].withDefault{new GraphErrors(it)}
  def grrun = [:].withDefault{[:].withDefault{new GraphErrors(it)}}

  runmap.each{run, flist ->
    def data = [:]
    flist.each{entry ->
      println id
      println entry
      entry.data.each{kk, vv ->
        grrun[run][kk].addPoint(entry.fnum, vv/fc[run][entry.fnum], 0, Math.sqrt(vv)/fc[run][entry.fnum])
        if(data.containsKey(kk))
          data[kk]+=vv
        else
          data[kk]=vv
      }
    }
    data.each{kk, vv ->
      gr[kk].addPoint(run, vv/fc[run].tot, 0, Math.sqrt(vv)/fc[run].tot)
    }
  }

  def out = new TDirectory()
  out.mkdir('/timelines')
  out.cd('/timelines')
  gr.each{out.addDataSet(it.value)}
  grrun.each{run, grs ->
    out.mkdir('/'+run)
    out.cd('/'+run)
    grs.each{out.addDataSet(it.value)}
  }
  out.writeFile(id[1..-1].replace('/', '_')+".hipo")
}


/*
  def fname = hfiles.find{name->name.contains(run.toString())}

  def inp = new TDirectory()
  inp.readFile(fname)
  def hists = inp.getCompositeObjectList(inp)

  rundata[run].plots = hists.findAll{name->name.contains(entry.id)}.collect{inp.getObject(it)}
*/
