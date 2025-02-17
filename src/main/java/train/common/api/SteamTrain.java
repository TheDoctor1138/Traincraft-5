package train.common.api;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import train.common.api.LiquidManager.StandardTank;
import train.common.core.handlers.FuelHandler;

public abstract class SteamTrain extends Locomotive implements IFluidHandler {

	protected int maxTank;
	private int maxFuel = 20000;
	private int update = 8;
	private StandardTank theTank;
	private IFluidTank[] tankArray = new IFluidTank[1];

	/**
	 * 
	 * @param world
	 */
	public SteamTrain(World world) {
		this(world, null);
	}


	public SteamTrain(World world, FluidStack filter) {
		super(world);
		this.maxTank = getTankCapacity()[0];
		if (filter == null) {
			this.theTank = LiquidManager.getInstance().new StandardTank(maxTank);
		} else {
			this.theTank = LiquidManager.getInstance().new FilteredTank(maxTank, filter);
		}
		tankArray[0] = theTank;
		dataWatcher.addObject(4, 0);
		this.dataWatcher.addObject(27, 0);
	}
	public SteamTrain(World world, double d, double d1, double d2) {
		super(world, d, d1, d2);
	}

	@Override
	public int getSizeInventory() {
		return 11+(getInventoryRows()*9);
	}

	/**
	 * returns the waterConsumption for each steam loco default is 200: rand.nextInt(200)==0
	 * 
	 * @return
	 */
	public int getWaterConsumption() {
		return getSpec().getWaterConsumption();
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		/**
		 * so the client side knows the water amount
		 */
		if (worldObj.isRemote) {
			return;
		}
		if (theTank != null && theTank.getFluid() != null) {
			this.dataWatcher.updateObject(27, theTank.getFluid().amount);
			this.dataWatcher.updateObject(4, theTank.getFluid().getFluidID());
		}

		if (theTank != null && theTank.getFluid() != null && getIsFuelled()) {
			if (theTank.getFluid().amount <= 1) {
				motionX *= 0.94;
				motionZ *= 0.94;
			}
		}
		else if (theTank != null && theTank.getFluid() == null) {
			this.dataWatcher.updateObject(27, 0);
			this.dataWatcher.updateObject(4, 0);
		}
		if (rand.nextInt(100) == 0 && getWater() > 0 && getIsFuelled()) {
			drain(ForgeDirection.UNKNOWN, getWaterConsumption() / 5, true);
		}

		checkInvent(cargoItems[0], cargoItems[1], this);
	}

	/**
	 * added for SMP, used by the HUD
	 * 
	 * @return
	 */
	public int getWater() {
		return (this.dataWatcher.getWatchableObjectInt(27));
	}

	/**
	 * used by the GUI
	 * 
	 * @return int
	 */
	public int getLiquidItemID() {
		return (this.dataWatcher.getWatchableObjectInt(4));
	}

	public StandardTank getTank() {
		return theTank;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		this.theTank.writeToNBT(nbttagcompound);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		this.theTank.readFromNBT(nbttagcompound);
	}

	public int getCartTankCapacity() {
		return maxTank;
	}

	private void placeInInvent(ItemStack itemstack1, SteamTrain loco) {
		for (int i = 2; i < loco.cargoItems.length; i++) {
			if (loco.cargoItems[i] == null) {
				loco.cargoItems[i] = itemstack1;
				return;
			}
			else if (loco.cargoItems[i] != null && loco.cargoItems[i].getItem() == itemstack1.getItem() && itemstack1.isStackable() &&
					(!itemstack1.getHasSubtypes() || cargoItems[i].getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(cargoItems[i], itemstack1)) {
				int var9 = cargoItems[i].stackSize + itemstack1.stackSize;
				if (var9 <= itemstack1.getMaxStackSize()) {
					loco.cargoItems[i].stackSize = var9;
					return;
				}
				else if (cargoItems[i].stackSize < cargoItems[i].getMaxStackSize()) {
					loco.cargoItems[i].stackSize += 1;
					return;
				}
			}
			else if (i == loco.cargoItems.length - 1) {
				entityDropItem(itemstack1,1);
				return;
			}
		}
	}

	public void liquidInSlot(ItemStack itemstack, SteamTrain loco) {

		if (worldObj.isRemote)
			return;
		this.update += 1;
		if (this.update % 8 == 0 && itemstack != null) {
			ItemStack result = LiquidManager.getInstance().processContainer(this, 1, this, itemstack); //'this' needs to be the loco inventory, but that's not an inventory it's a Itemstack[]
			if (result != null) {
				placeInInvent(result, loco);
				decrStackSize(1, 1);
			}
		}
	}

	protected void checkInvent(ItemStack locoInvent0, ItemStack locoInvent1, SteamTrain loco) {
		if (!this.canCheckInvent)
			return;

		boolean hasCoalInTender = false;
		if (isLocoTurnedOn() && ticksExisted%10==0) {
			FluidStack drain = null;

			if(fill(ForgeDirection.UNKNOWN,new FluidStack(FluidRegistry.WATER, 100), false)==100) {
				blocksToCheck = new TileEntity[]{worldObj.getTileEntity(MathHelper.floor_double(posX), MathHelper.floor_double(posY - 1), MathHelper.floor_double(posZ)),
						worldObj.getTileEntity(MathHelper.floor_double(posX), MathHelper.floor_double(posY + 2), MathHelper.floor_double(posZ)),
						worldObj.getTileEntity(MathHelper.floor_double(posX), MathHelper.floor_double(posY + 3), MathHelper.floor_double(posZ)),
						worldObj.getTileEntity(MathHelper.floor_double(posX), MathHelper.floor_double(posY + 4), MathHelper.floor_double(posZ))
				};

				for (TileEntity block : blocksToCheck) {
					if (drain == null && block instanceof IFluidHandler) {
						for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
							if(((IFluidHandler) block).drain(direction,100,false)!=null &&
									((IFluidHandler) block).drain(direction, 100, false).fluid==FluidRegistry.WATER &&
									((IFluidHandler) block).drain(direction, 100, false).amount ==100
							) {
								drain = ((IFluidHandler) block).drain(
										direction, 100, true);
							}
						}
					}
				}
			}

			if(cartLinked1 instanceof Tender){
				if(drain==null && fill(ForgeDirection.UNKNOWN,new FluidStack(FluidRegistry.WATER, 100), false)==100) {
					if (getFluid() == null || getFluid().getFluid() == FluidRegistry.WATER) {
						drain = cartLinked1.drain(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 100), true);
					}
				}
				for (int h = 0; h < ((Tender) cartLinked1).tenderItems.length; h++) {
					if (((Tender) cartLinked1).tenderItems[h] != null && FuelHandler.steamFuelLast(((Tender) cartLinked1).tenderItems[h]) != 0) {
						if (getFuel() < maxFuel && ((getFuel() + FuelHandler.steamFuelLast(((Tender) cartLinked1).tenderItems[h])) <= maxFuel)) {
							fuelTrain += FuelHandler.steamFuelLast(((Tender) cartLinked1).tenderItems[h]);
							hasCoalInTender = true;
							cartLinked1.decrStackSize(h, 1);
							break;
						}
					}
				}


			} else if (cartLinked2 instanceof Tender){

				if(drain==null && fill(ForgeDirection.UNKNOWN,new FluidStack(FluidRegistry.WATER, 100), false)==100) {
					if (getFluid() == null || getFluid().getFluid() == FluidRegistry.WATER) {
						drain = cartLinked2.drain(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 100), true);
					}
				}


				for (int h = 0; h < ((Tender) cartLinked2).tenderItems.length; h++) {
					if (((Tender) cartLinked2).tenderItems[h] != null && FuelHandler.steamFuelLast(((Tender) cartLinked2).tenderItems[h]) != 0) {
						if (getFuel() < maxFuel && ((getFuel() + FuelHandler.steamFuelLast(((Tender) cartLinked2).tenderItems[h])) <= maxFuel)) {
							fuelTrain += FuelHandler.steamFuelLast(((Tender) cartLinked2).tenderItems[h]);
							hasCoalInTender = true;
							cartLinked2.decrStackSize(h, 1);
							break;
						}
					}
				}
			}
			if (drain != null){
				fill(ForgeDirection.UNKNOWN, drain, true);
			}
		}
		if (!hasCoalInTender && locoInvent0 != null && FuelHandler.steamFuelLast(locoInvent0) != 0) {
			if (getFuel() < maxFuel && ((getFuel() + FuelHandler.steamFuelLast(locoInvent0) <= maxFuel))) {
				fuelTrain += FuelHandler.steamFuelLast(locoInvent0);
				decrStackSize(0, 1);
			}
		}


		if (locoInvent1 != null) {
			liquidInSlot(locoInvent1, loco);
			return;
		}
		if (getFuel() <= 0) {
			motionX *= 0.88;
			motionZ *= 0.88;
		}
	}

	/** Used for the gui */
	@Override
	public int getFuelDiv(int i) {
		if (worldObj.isRemote) {
			return ((this.dataWatcher.getWatchableObjectInt(24) * i) / maxFuel);
		}
		return (this.fuelTrain * i) / maxFuel;
	}

	public void setCapacity(int capacity) {
		this.maxTank = capacity;
	}

	public int getCapacity() {
		return this.maxTank;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		return theTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		if (resource == null || !resource.isFluidEqual(theTank.getFluid())) {
			return null;
		}
		return theTank.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return theTank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return true;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return true;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return new FluidTankInfo[] { theTank.getInfo() };
	}

	public FluidStack getFluid() {
		return theTank.getFluid();
	}

	public int getFluidAmount() {
		return theTank.getFluidAmount();
	}
}