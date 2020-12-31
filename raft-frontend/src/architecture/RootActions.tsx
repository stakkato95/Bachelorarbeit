export const UPDATE_VALUE_TO_REPLICATE = "UPDATE_VALUE_TO_REPLICATE";

export const GET_CLUSTER_STATE = "GET_CLUSTER_STATE";
export const SET_CLUSTER_STATE = "SET_CLUSTER_STATE";
export const REPLICATE_VALUE = "REPLICATE_VALUE";

export const updateValueToReplicate = (value: any) => ({ value: value, type: UPDATE_VALUE_TO_REPLICATE });

export const getClusterState = () => ({ type: GET_CLUSTER_STATE });
export const setClusterState = (state: any) => ({ state: state, type: SET_CLUSTER_STATE });
export const replicateValue = () => ({ type: REPLICATE_VALUE });