export const GET_CLUSTER_STATE = "GET_CLUSTER_STATE";
export const SET_CLUSTER_STATE = "SET_CLUSTER_STATE";

export const getClusterState = () => ({ type: GET_CLUSTER_STATE });
export const setClusterState = (state: any) => ({ state: state, type: SET_CLUSTER_STATE });