package com.lon.outsidemonitor.signal;



public interface ISignal {

public SignalType getSignalType();
	
	/*
	 * ��ȡ��������
	 */
	public float getACAmpl();
	/*
	 * ��ȡֱ������
	 */
	public float getDCAmpl();
	
	public String getSignalInfo();
	
	public void copyTo(ISignal dest);
	
	public short[] getRawData();
	public float[] getSpectrumData();
	
	public SignalAmpl getAmpl();
	
	public String getUnit();
	
	public long getTime();
	
	public void setParam(boolean enableDC,String unit);
}