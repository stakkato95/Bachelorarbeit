import {
    SET_CLUSTER_STATE,
    UPDATE_VALUE_TO_REPLICATE
} from './RootActions';

export const sessionInitialState = {
    clusterState: {},
    valueToReplicate: "",
};

export const reducer = (state = sessionInitialState, action: any) => {
    switch (action.type) {
        case SET_CLUSTER_STATE:
            return Object.assign({}, state, { clusterState: action.state });
        case UPDATE_VALUE_TO_REPLICATE:
            return Object.assign({}, state, { valueToReplicate: action.value });
    }
    return state;
};