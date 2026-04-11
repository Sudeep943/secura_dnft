package com.secura.dnft.request.response;

import java.util.List;

public class GetAllFlatsResponse {

	private GenericHeader genericHeader;
	private List<BlockDetails> blockList;
	private List<TowerDetails> towerList;
	private String message;
	private String messageCode;

	public GenericHeader getGenericHeader() {
		return genericHeader;
	}

	public void setGenericHeader(GenericHeader genericHeader) {
		this.genericHeader = genericHeader;
	}

	public List<BlockDetails> getBlockList() {
		return blockList;
	}

	public void setBlockList(List<BlockDetails> blockList) {
		this.blockList = blockList;
	}

	public List<TowerDetails> getTowerList() {
		return towerList;
	}

	public void setTowerList(List<TowerDetails> towerList) {
		this.towerList = towerList;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(String messageCode) {
		this.messageCode = messageCode;
	}

	public static class BlockDetails {
		private String blockName;
		private List<TowerDetails> towerList;
		private List<String> flatList;

		public String getBlockName() {
			return blockName;
		}

		public void setBlockName(String blockName) {
			this.blockName = blockName;
		}

		public List<TowerDetails> getTowerList() {
			return towerList;
		}

		public void setTowerList(List<TowerDetails> towerList) {
			this.towerList = towerList;
		}

		public List<String> getFlatList() {
			return flatList;
		}

		public void setFlatList(List<String> flatList) {
			this.flatList = flatList;
		}
	}

	public static class TowerDetails {
		private String towerName;
		private List<String> flatList;

		public String getTowerName() {
			return towerName;
		}

		public void setTowerName(String towerName) {
			this.towerName = towerName;
		}

		public List<String> getFlatList() {
			return flatList;
		}

		public void setFlatList(List<String> flatList) {
			this.flatList = flatList;
		}
	}
}
