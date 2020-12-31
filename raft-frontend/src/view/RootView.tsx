import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { makeStyles } from '@material-ui/core/styles';
import Paper from '@material-ui/core/Paper';
import Grid from '@material-ui/core/Grid';

import { getClusterState } from '../architecture/RootActions';

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        padding: '16px'
    },
    paper: {
        padding: theme.spacing(2),
        textAlign: 'center',
        color: theme.palette.text.secondary,
    },
}));

export default function RootView() {
    const clusterState = useSelector((state: any) => state.clusterState);
    const classes = useStyles();
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(getClusterState());
    });

    if (clusterState.leader === undefined) {
        return (<div>loading...</div>)
    }

    return (
        <div className={classes.root}>
            <Grid container spacing={3}>
                <Grid item xs>
                    <Paper className={classes.paper}>{clusterState.leader.nodeId}</Paper>
                </Grid>
                <Grid item xs>
                    <Paper className={classes.paper}>xs</Paper>
                </Grid>
                <Grid item xs>
                    <Paper className={classes.paper}>xs</Paper>
                </Grid>
            </Grid>
        </div>
    );
}