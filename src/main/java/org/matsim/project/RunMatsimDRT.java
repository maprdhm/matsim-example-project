package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.flow.AvIncreasedCapacityModule;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.contrib.drt.extension.edrt.run.EDrtControlerCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup.BoardingAcceptance;
import org.matsim.vehicles.VehicleType;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunMatsimDRT {

	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8; // charge up to 80% SOC
	private static final double MIN_RELATIVE_SOC = 0.2; // send to chargers vehicles below 20% SOC
	private static final double TEMPERATURE = 20; // oC
	
	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios_v1/winnipeg/2022/input/weekday/weekday_2022_config_25pct_DRT.xml", new MultiModeDrtConfigGroup(), 
					new DvrpConfigGroup(), new OTFVisConfigGroup(),new EvConfigGroup());
		} else {
			config = ConfigUtils.loadConfig(args, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup(),new EvConfigGroup());
		}
	
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		if (config.qsim().getFlowCapFactor()<0.5) {
			config.qsim().setPcuThresholdForFlowCapacityEasing(0.3);
			config.transit().setBoardingAcceptance(BoardingAcceptance.checkStopOnly);
		}
		
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);
		
		Controler controler = EDrtControlerCreator.createControler(config, false) ;

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
				@Override
				public void install() {
					bind(EDrtVehicleDataEntryFactoryProvider.class).toInstance(
							new EDrtVehicleDataEntryFactoryProvider(drtCfg, MIN_RELATIVE_SOC));
				}
			});
		}

		VehicleType drtAvType = scenario.getVehicles().getVehicleTypes().get(Id.get("drt_av", VehicleType.class ));
		controler.addOverridingModule(new AvIncreasedCapacityModule("drt_av", 2.0,drtAvType));

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).toProvider(new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
						charger -> new ChargeUpToMaxSocStrategy(charger, MAX_RELATIVE_SOC)));
				bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, CHARGING_SPEED_FACTOR));
				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);
			}
		});
		
		controler.run();
		
	}
}
