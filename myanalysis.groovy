#!/home/kenjo/groovy/coatjava/bin/run-groovy
import org.jlab.groot.data.TDirectory
import org.jlab.io.hipo.HipoDataSource
import pid.electron.Electron
import groovy.sql.Sql
import org.jlab.groot.data.H1F
import org.jlab.clas.physics.Vector3
import org.jlab.clas.physics.Particle
import org.jlab.detector.base.DetectorType

for (fname in arg){
  int run = fname.split("/")[-1].split('\\.')[0][-4.,-1].toInteger()
  int fnum = fname.split(".evio.")[-1].split('-')[0].split("\\.")[0].toInteger()
	
  def plots = [:].withDefault{new H1F("elec_$it", "electron theta", 90, 0, 45)}} 
  def data  = [:]

  def reader = new HipoDataSource()
  reader.open(fname)
	
  while(reader.hasEvent()) {
    def event = reader.getNextEvent()
    if (event.hasBank("REC::Particle") && event.hasBank("REC::Calorimeter")) {
      def pbank = event.getBank("REC::Particle")
      def calbank = event.getBanl("REC::Calorimeter")
			
      int eleind = (0..<pbank.rows()).find{pbank.getInt("pid",it)==11 && 
                                            pbank.getShort("status",it)<0}
      def elesec = (0..<calbank.rows()).collect{
                    (calbank.getShort('pindex',it).toInteger() == eleind &&
                    calbank.getByte('detector',it).toInteger() == DectectorType.ECAL.getDetectorId()
                    ? calbank.getByte('sector',it) : null }.find()
      if( eleind != null && elesec != null ) {
        def electron = new Particle(11, *['px', 'py', 'pz'].collect{pbank.getFloat(it, eleind)})
        data.trigger["sec$elesec"]++
      }
    }
  }
  
  reader.close()
  
  def out = new TDirectory()
  plots.each{name, dss ->
    out.mkdir("/electron/$name")
    out.cd("/electron/$name")
    dss.values().each{out.addDataSet(it)}
  out.writeFile(String.format("monplot_paul_%d_%05d.hipo", run, fnum))
}

