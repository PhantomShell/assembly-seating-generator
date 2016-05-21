package com;

import java.util.Comparator;


public class RoomFillComparator implements Comparator<Room> {
	
	@Override
	public int compare(Room room1, Room room2) {
		if (room1.isFull() && !room2.isFull()) {
			return 1;
		}
		if (!room1.isFull() && room2.isFull()) {
			return -1;
		}
		return ((Integer) room1.getFilledSeats()).compareTo(room2.getFilledSeats());
	}

}